package com.github.niklaswortmann.knipintellijplugin.lsp

import com.github.niklaswortmann.knipintellijplugin.KnipBundle
import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettings
import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettingsConfigurable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerState
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem
import com.intellij.platform.util.progress.reportProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

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

            // Register file change listener to send didChangeWatchedFiles notifications
            // This is a workaround for IntelliJ not automatically sending these notifications
            KnipFileChangeListener.register(project)

            // Show progress indicator until module graph is built
            startKnipSessionWithProgress(project)
        }
    }

    override fun createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?): LspServerWidgetItem {
        return LspServerWidgetItem(
            lspServer,
            currentFile,
            KNIP_ICON,
            settingsPageClass = KnipSettingsConfigurable::class.java
        )
    }
    
    /**
     * Shows a progress indicator while Knip analyzes the project.
     * Uses a managed CoroutineScope with SupervisorJob for proper lifecycle management.
     *
     * Only one progress indicator is shown at a time per project. If a new progress session
     * is started while one is already running (e.g., on server restart), the existing one
     * is cancelled first.
     *
     * The progress indicator is only shown for language server version 1.1.0 and above,
     * which supports the knip.moduleGraphBuilt notification. Older versions don't show
     * a progress indicator.
     */
    @Suppress("UnstableApiUsage")
    private fun startKnipSessionWithProgress(project: Project) {
        val projectPath = project.basePath ?: return

        // Cancel any existing progress job for this project
        activeProgressJobs[projectPath]?.let { existingJob ->
            LOG.info("Cancelling existing progress indicator for project: $projectPath")
            existingJob.cancel()
        }

        // Reset the future for a fresh start
        KnipLspServerDescriptor.resetModuleGraphBuiltFuture(projectPath)

        // Create a supervised scope for the progress job
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val job = scope.launch {
            try {
                // First, wait for server to be ready and check version
                waitForServerReady(project)

                val serverVersion = getServerVersion(project)
                LOG.info("Knip language server version: ${serverVersion ?: "unknown"}")

                // Send knip.start to trigger analysis (required for all versions)
                sendKnipStartRequest(project)

                // Only show progress bar for version 1.1.0 and above
                if (serverVersion == null || !isVersionAtLeast(serverVersion, MIN_VERSION_FOR_PROGRESS)) {
                    LOG.info("Skipping progress indicator: server version $serverVersion does not support moduleGraphBuilt notification (requires $MIN_VERSION_FOR_PROGRESS+)")
                    return@launch
                }

                withBackgroundProgress(project, KnipBundle.message("progressTitle"), cancellable = true) {
                    LOG.info("Knip startup progress indicator started")

                    reportProgress(1) { reporter ->
                        // Wait for module graph to be built (knip.moduleGraphBuilt notification)
                        reporter.itemStep(KnipBundle.message("progressBuildingModuleGraph")) {
                            waitForModuleGraphBuilt(projectPath)
                        }
                    }

                    LOG.info("Knip startup progress indicator finished")
                }
            } finally {
                // Clean up: remove this job from the map when done
                activeProgressJobs.remove(projectPath, scope.coroutineContext[Job])
            }
        }

        // Track the new job
        activeProgressJobs[projectPath] = job
    }

    /**
     * Gets the language server version from the LSP server's initialization result.
     * Returns null if the version cannot be determined.
     */
    private fun getServerVersion(project: Project): String? {
        val lspServerManager = LspServerManager.getInstance(project)
        val servers = lspServerManager.getServersForProvider(KnipLspServerSupportProvider::class.java)

        for (server in servers) {
            if (server.state == LspServerState.Running) {
                val initResult = server.initializeResult
                if (initResult == null) {
                    LOG.warn("Server is running but initializeResult is null")
                    return null
                }
                val serverInfo = initResult.serverInfo
                LOG.info("Server initializeResult.serverInfo: $serverInfo")
                if (serverInfo == null) {
                    LOG.warn("Server initializeResult has no serverInfo - language server may not report version")
                    return null
                }
                return serverInfo.version
            }
        }
        LOG.warn("No running server found when checking version")
        return null
    }

    /**
     * Sends the knip.start request to trigger analysis.
     * This is required for all server versions to begin analyzing the project.
     */
    private suspend fun sendKnipStartRequest(project: Project) {
        try {
            val lspServerManager = LspServerManager.getInstance(project)
            val servers = lspServerManager.getServersForProvider(KnipLspServerSupportProvider::class.java)

            for (lspServer in servers) {
                if (lspServer.state == LspServerState.Running) {
                    LOG.info("Sending knip.start request to trigger analysis")
                    lspServer.sendRequest<Any?> { server ->
                        if (server is KnipLanguageServer) {
                            server.knipStart()
                        } else {
                            LOG.warn("Server is not a KnipLanguageServer instance")
                            java.util.concurrent.CompletableFuture.completedFuture(null)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Error sending knip.start request: ${e.message}", e)
        }
    }

    /**
     * Waits for the knip.moduleGraphBuilt notification from the language server.
     * This indicates that Knip has finished analyzing the project.
     * Times out after 5 minutes to prevent indefinite waiting.
     */
    private suspend fun waitForModuleGraphBuilt(projectPath: String) {
        val future = KnipLspServerDescriptor.getModuleGraphBuiltFuture(projectPath)
        val timeoutMs = 5 * 60 * 1000L // 5 minutes

        val result = withTimeoutOrNull(timeoutMs) {
            future.await()
        }

        if (result == null) {
            LOG.warn("Timeout waiting for knip.moduleGraphBuilt notification after ${timeoutMs}ms")
        } else {
            LOG.info("Module graph built successfully")
        }
    }
    
    /**
     * Waits for the LSP server to be in a running state.
     * Polls every 100ms with a maximum timeout of 30 seconds.
     */
    private suspend fun waitForServerReady(project: Project) {
        val maxWaitMs = 30_000L
        val pollIntervalMs = 100L
        var waited = 0L
        
        while (waited < maxWaitMs) {
            val lspServerManager = LspServerManager.getInstance(project)
            val servers = lspServerManager.getServersForProvider(KnipLspServerSupportProvider::class.java)
            
            // Check if any server is running
            val hasRunningServer = servers.any { server ->
                server.state == LspServerState.Running
            }
            
            if (hasRunningServer) {
                LOG.info("Knip LSP server is ready after ${waited}ms")
                return
            }
            
            delay(pollIntervalMs)
            waited += pollIntervalMs
        }
        
        LOG.warn("Timeout waiting for Knip LSP server to be ready after ${maxWaitMs}ms")
    }

    companion object {
        private val LOG = Logger.getInstance(KnipLspServerSupportProvider::class.java)
        private val KNIP_ICON = IconLoader.getIcon("/icons/knip_16.png", KnipLspServerSupportProvider::class.java)

        /**
         * Minimum language server version that supports the knip.moduleGraphBuilt notification.
         */
        private const val MIN_VERSION_FOR_PROGRESS = "1.1.0"

        /**
         * Map to track active progress jobs per project path.
         * Ensures only one progress indicator is shown at a time per project.
         */
        private val activeProgressJobs = ConcurrentHashMap<String, Job>()

        /**
         * Compares two semantic version strings.
         * Returns true if [version] is at least [minVersion].
         */
        internal fun isVersionAtLeast(version: String, minVersion: String): Boolean {
            try {
                val versionParts = version.split(".").map { it.toIntOrNull() ?: 0 }
                val minParts = minVersion.split(".").map { it.toIntOrNull() ?: 0 }

                for (i in 0 until maxOf(versionParts.size, minParts.size)) {
                    val v = versionParts.getOrElse(i) { 0 }
                    val m = minParts.getOrElse(i) { 0 }
                    if (v > m) return true
                    if (v < m) return false
                }
                return true // versions are equal
            } catch (e: Exception) {
                LOG.warn("Failed to parse version string: $version", e)
                return false
            }
        }

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

            // Check Knip-specific config file patterns
            if (fileName == "knip.ts" ||
                fileName == "knip.config.ts" ||
                fileName == "knip.config.js" ||
                fileName == "knip.config.mjs" ||
                fileName == "knip.config.cjs"
            ) {
                return true
            }

            return false
        }
    }
}
