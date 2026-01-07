package com.github.niklaswortmann.knipintellijplugin.lsp

import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettings
import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettingsConfigurable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * LSP Server Support Provider for the Knip language server.
 * This is the entry point for the Platform LSP API integration.
 * 
 * When a file is opened, this provider checks if it's a supported file type
 * and starts the Knip language server if needed.
 */
class KnipLspServerSupportProvider : LspServerSupportProvider {

    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        // Check if Knip is enabled in settings
        val settings = KnipSettings.getInstance(project)
        if (!settings.enabled) {
            return
        }

        // Check if this is a supported file type
        if (isSupportedFile(file)) {
            val descriptor = KnipLspServerDescriptor(project)
            serverStarter.ensureServerStarted(descriptor)
            
            // Send knip.start request after server is started with progress indicator
            startKnipSessionWithProgress(project)
        }
    }

    override fun createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?): LspServerWidgetItem {
        return LspServerWidgetItem(
            lspServer,
            currentFile,
            settingsPageClass = KnipSettingsConfigurable::class.java
        )
    }
    
    /**
     * Starts the Knip session with a progress indicator in the IDE status bar.
     */
    private fun startKnipSessionWithProgress(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(object : Task.Backgroundable(
                project,
                PROGRESS_TITLE,
                true  // cancellable
            ) {
                private val analysisComplete = AtomicBoolean(false)
                
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    indicator.text = PROGRESS_STARTING
                    
                    LOG.info("Knip startup progress indicator started")
                    
                    // Launch coroutine to send knip.start request
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Wait for server to be fully initialized
                            delay(2000)
                            
                            // Update progress message
                            indicator.text = PROGRESS_ANALYZING
                            
                            sendKnipStartRequest(project)
                            
                            LOG.info("Knip startup progress indicator finished")
                        } finally {
                            analysisComplete.set(true)
                        }
                    }
                    
                    // Wait for analysis to complete or cancellation
                    while (!analysisComplete.get() && !indicator.isCanceled) {
                        Thread.sleep(100)
                    }
                }
                
                override fun onCancel() {
                    LOG.info("Knip startup progress was cancelled by user")
                    analysisComplete.set(true)
                }
            })
        }
    }
    
    /**
     * Sends the knip.start request to the language server.
     * This is required to initialize the Knip session and start publishing diagnostics.
     */
    private suspend fun sendKnipStartRequest(project: Project) {
        try {
            val lspServerManager = LspServerManager.getInstance(project)
            val servers = lspServerManager.getServersForProvider(KnipLspServerSupportProvider::class.java)
            
            for (lspServer in servers) {
                // Use sendRequest to send custom request to the server
                LOG.info("Sending knip.start request to initialize Knip session")
                try {
                    lspServer.sendRequest<Any?> { server ->
                        if (server is KnipLanguageServer) {
                            server.knipStart()
                        } else {
                            LOG.warn("Server is not a KnipLanguageServer instance")
                            java.util.concurrent.CompletableFuture.completedFuture(null)
                        }
                    }
                    LOG.info("Knip session started successfully")
                } catch (e: Exception) {
                    LOG.warn("Failed to start Knip session: ${e.message}")
                }
            }
        } catch (e: Exception) {
            LOG.warn("Error sending knip.start request: ${e.message}", e)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(KnipLspServerSupportProvider::class.java)
        
        // Progress messages
        private const val PROGRESS_TITLE = "Knip"
        private const val PROGRESS_STARTING = "Starting language server..."
        private const val PROGRESS_ANALYZING = "Analyzing project for unused code..."
        
        /**
         * Supported file extensions for the Knip language server.
         */
        private val SUPPORTED_EXTENSIONS = setOf(
            "js", "jsx", "ts", "tsx", "mjs", "cjs", "mts", "cts"
        )

        /**
         * Supported file names (exact match).
         */
        private val SUPPORTED_FILE_NAMES = setOf(
            "package.json", "knip.json", "knip.jsonc"
        )

        /**
         * Checks if the given file is supported by the Knip language server.
         */
        fun isSupportedFile(file: VirtualFile): Boolean {
            val fileName = file.name
            val extension = file.extension?.lowercase()

            // Check exact file names
            if (fileName in SUPPORTED_FILE_NAMES) {
                return true
            }

            // Check extensions
            if (extension in SUPPORTED_EXTENSIONS) {
                return true
            }

            // Check config file patterns
            if (fileName.endsWith(".config.js") ||
                fileName.endsWith(".config.ts") ||
                fileName.endsWith(".config.mjs") ||
                fileName.endsWith(".config.cjs") ||
                fileName == "knip.ts"
            ) {
                return true
            }

            return false
        }

        /**
         * Restarts the Knip language server asynchronously.
         */
        fun restartServerAsync(project: Project) {
            ApplicationManager.getApplication().invokeLater({
                LspServerManager.getInstance(project)
                    .stopAndRestartIfNeeded(KnipLspServerSupportProvider::class.java)
            }, project.disposed)
        }
    }
}
