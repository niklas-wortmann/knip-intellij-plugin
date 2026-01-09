package com.github.niklaswortmann.knipintellijplugin.lsp

import com.github.niklaswortmann.knipintellijplugin.KnipBundle
import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettings
import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettingsConfigurable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
     * Uses a managed CoroutineScope with SupervisorJob for proper lifecycle management.
     * The scope is cancelled after the work completes to prevent memory leaks.
     */
    @Suppress("UnstableApiUsage")
    private fun startKnipSessionWithProgress(project: Project) {
        // Create a supervised scope that will be cancelled after work completes
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                withBackgroundProgress(project, KnipBundle.message("progressTitle"), cancellable = true) {
                    LOG.info("Knip startup progress indicator started")
                    
                    reportProgress(2) { reporter ->
                        // Step 1: Wait for server to be fully initialized
                        reporter.itemStep(KnipBundle.message("progressStartingServer")) {
                            waitForServerReady(project)
                        }
                        
                        // Step 2: Send knip.start request to initialize session
                        reporter.itemStep(KnipBundle.message("progressInitializingSession")) {
                            sendKnipStartRequest(project)
                        }
                    }
                    
                    LOG.info("Knip startup progress indicator finished")
                }
            } finally {
                // Cancel the scope to clean up resources
                scope.cancel()
            }
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
