package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerState
import com.intellij.util.Alarm
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.FileChangeType
import org.eclipse.lsp4j.FileEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * File change listener that sends workspace/didChangeWatchedFiles notifications
 * to the Knip language server when files are modified.
 *
 * This is a workaround for IntelliJ's LSP API not automatically sending
 * these notifications (see IJPL-15742).
 *
 * The Knip language server relies on didChangeWatchedFiles to detect changes
 * and republish diagnostics for unused exports.
 *
 * IMPORTANT: The Knip server reads files from DISK, not from editor buffers.
 * Since JetBrains IDEs auto-save files (users don't manually save), we:
 * 1. Listen for document changes in the editor
 * 2. Debounce changes (wait for typing to pause)
 * 3. Force-save the document to disk
 * 4. Send the didChangeWatchedFiles notification
 *
 * This gives a seamless experience where diagnostics update shortly after typing stops.
 */
class KnipFileChangeListener(
    private val project: Project
) : BulkFileListener, DocumentListener, Disposable {

    private val connection = project.messageBus.connect(this)
    private var isDisposed = false

    // Alarm for debouncing document changes (wait for typing to stop)
    private val saveAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    // Track documents with pending saves to debounce properly
    private val pendingDocuments = ConcurrentHashMap<Document, Runnable>()

    // Debounce delay in milliseconds (wait this long after last keystroke)
    private val debounceDelayMs = 1000

    init {
        // Listen for VFS changes (create, delete, move, and content changes after save)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this)

        // Listen for document changes in editors (typing)
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(this, this)

        LOG.info("KnipFileChangeListener registered for project: ${project.name}")
    }

    /**
     * Called when a document is changed (user is typing).
     * We debounce these changes and force-save after the user stops typing.
     */
    override fun documentChanged(event: DocumentEvent) {
        if (isDisposed) return

        val document = event.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return

        // Only process files within the project
        val projectBasePath = project.basePath ?: return
        if (!file.path.startsWith(projectBasePath)) return

        // Skip ignored files
        if (shouldIgnoreFile(file)) return

        // Only watch files that Knip cares about
        if (!isWatchedFile(file)) return

        // Cancel any pending save for this document
        pendingDocuments[document]?.let { saveAlarm.cancelRequest(it) }

        // Schedule a new save after the debounce delay
        val saveTask = Runnable {
            if (!isDisposed && !project.isDisposed) {
                saveDocumentAndNotify(document, file)
            }
            pendingDocuments.remove(document)
        }

        pendingDocuments[document] = saveTask
        saveAlarm.addRequest(saveTask, debounceDelayMs)
    }

    /**
     * Saves the document to disk and sends the notification.
     * Must be called on the EDT for document saving.
     */
    private fun saveDocumentAndNotify(document: Document, file: VirtualFile) {
        ApplicationManager.getApplication().invokeLater {
            if (isDisposed || project.isDisposed) return@invokeLater

            val fileDocumentManager = FileDocumentManager.getInstance()

            // Only save if the document is still modified
            if (fileDocumentManager.isDocumentUnsaved(document)) {
                LOG.info("Auto-saving document for Knip: ${file.path}")
                fileDocumentManager.saveDocument(document)
                // The VFS event will be triggered by the save, which will send the notification
            }
        }
    }

    /**
     * Handles VFS events for file operations.
     * VFileContentChangeEvent fires AFTER the file is written to disk.
     */
    override fun after(events: MutableList<out VFileEvent>) {
        if (isDisposed) return

        val fileEvents = mutableListOf<FileEvent>()

        for (event in events) {
            val file = event.file ?: continue

            // Only process files within the project
            val projectBasePath = project.basePath ?: continue
            if (!file.path.startsWith(projectBasePath)) continue

            // Skip files in node_modules, .git, etc.
            if (shouldIgnoreFile(file)) continue

            // Only watch files that Knip cares about
            if (!isWatchedFile(file)) continue

            val changeType = when (event) {
                is VFileCreateEvent -> FileChangeType.Created
                is VFileDeleteEvent -> FileChangeType.Deleted
                is VFileContentChangeEvent -> FileChangeType.Changed
                is VFileMoveEvent -> {
                    // For moves, report delete of old path and create of new path
                    fileEvents.add(FileEvent(event.oldPath.toFileUri(), FileChangeType.Deleted))
                    FileChangeType.Created
                }
                is VFileCopyEvent -> FileChangeType.Created
                is VFilePropertyChangeEvent -> {
                    // Property changes (like rename)
                    if (event.propertyName == VirtualFile.PROP_NAME) {
                        // For renames, we need old and new paths
                        val oldPath = file.parent?.path + "/" + event.oldValue
                        fileEvents.add(FileEvent(oldPath.toFileUri(), FileChangeType.Deleted))
                        FileChangeType.Created
                    } else {
                        continue
                    }
                }
                else -> continue
            }

            val uri = file.path.toFileUri()
            fileEvents.add(FileEvent(uri, changeType))
            LOG.info("VFS event: $uri (${changeType.name})")
        }

        if (fileEvents.isNotEmpty()) {
            sendDidChangeWatchedFilesNotification(fileEvents)
        }
    }

    /**
     * Sends the workspace/didChangeWatchedFiles notification to all running Knip servers.
     */
    private fun sendDidChangeWatchedFilesNotification(fileEvents: List<FileEvent>) {
        try {
            val lspServerManager = LspServerManager.getInstance(project)
            val servers = lspServerManager.getServersForProvider(KnipLspServerSupportProvider::class.java)

            for (server in servers) {
                if (server.state == LspServerState.Running) {
                    LOG.info("Sending didChangeWatchedFiles notification with ${fileEvents.size} events")
                    server.sendNotification { languageServer ->
                        val params = DidChangeWatchedFilesParams(fileEvents)
                        languageServer.workspaceService.didChangeWatchedFiles(params)
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Error sending didChangeWatchedFiles notification: ${e.message}", e)
        }
    }

    /**
     * Checks if the file should be ignored (node_modules, .git, etc.)
     */
    private fun shouldIgnoreFile(file: VirtualFile): Boolean {
        val path = file.path
        return IGNORED_PATHS.any { path.contains(it) }
    }

    /**
     * Checks if the file is one that Knip cares about.
     * Includes JS/TS files and configuration files.
     */
    private fun isWatchedFile(file: VirtualFile): Boolean {
        val fileName = file.name
        val extension = file.extension?.lowercase()

        // Watch JS/TS files
        if (extension in WATCHED_EXTENSIONS) {
            return true
        }

        // Watch configuration files
        if (fileName in WATCHED_FILE_NAMES) {
            return true
        }

        return false
    }

    override fun dispose() {
        isDisposed = true
        LOG.info("KnipFileChangeListener disposed for project: ${project.name}")
    }

    companion object {
        private val LOG = Logger.getInstance(KnipFileChangeListener::class.java)

        /**
         * Paths that should be ignored for file watching.
         */
        private val IGNORED_PATHS = setOf(
            "/node_modules/",
            "/.git/",
            "/dist/",
            "/build/",
            "/.idea/",
            "/coverage/",
            "/.next/",
            "/.nuxt/",
            "/out/"
        )

        /**
         * File extensions to watch for changes.
         */
        private val WATCHED_EXTENSIONS = setOf(
            "js", "jsx", "ts", "tsx", "mjs", "cjs", "mts", "cts", "json"
        )

        /**
         * Specific file names to watch.
         */
        private val WATCHED_FILE_NAMES = setOf(
            "package.json",
            "tsconfig.json",
            "jsconfig.json",
            "knip.json",
            "knip.jsonc",
            "knip.ts",
            "knip.config.ts",
            "knip.config.js"
        )

        /**
         * Converts a file path to a file:// URI.
         */
        private fun String.toFileUri(): String {
            return "file://$this"
        }

        /**
         * Map to track active listeners per project.
         */
        private val activeListeners = mutableMapOf<String, KnipFileChangeListener>()

        /**
         * Registers a file change listener for the given project.
         * Only one listener is registered per project.
         */
        fun register(project: Project): KnipFileChangeListener {
            val projectPath = project.basePath ?: project.name
            return activeListeners.getOrPut(projectPath) {
                KnipFileChangeListener(project)
            }
        }

        /**
         * Unregisters the file change listener for the given project.
         */
        fun unregister(project: Project) {
            val projectPath = project.basePath ?: project.name
            activeListeners.remove(projectPath)?.dispose()
        }
    }
}
