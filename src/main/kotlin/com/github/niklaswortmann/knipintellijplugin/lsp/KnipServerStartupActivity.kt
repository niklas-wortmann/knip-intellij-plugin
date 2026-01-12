package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.ExecuteCommandParams
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles the Knip language server session startup.
 * Polls for server state and sends the knip.start command when the server is ready.
 */
object KnipServerStartupHandler {
    private val LOG = Logger.getInstance(KnipServerStartupHandler::class.java)
    private val startupInProgress = ConcurrentHashMap<String, Boolean>()

    /**
     * Initiates the server startup sequence for the given project.
     * This should be called when the LSP server support provider creates a descriptor.
     */
    fun initiateStartup(project: Project) {
        val projectPath = project.basePath ?: project.name

        // Prevent duplicate startup attempts
        if (startupInProgress.putIfAbsent(projectPath, true) != null) {
            LOG.info("Knip startup already in progress for project: $projectPath, skipping")
            return
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                waitForServerAndStart(project)
            } finally {
                startupInProgress.remove(projectPath)
                scope.cancel()
            }
        }
    }

    /**
     * Waits for the server to be ready and sends the knip.start command.
     */
    private suspend fun waitForServerAndStart(project: Project) {
        val maxWaitMs = 30_000L
        val pollIntervalMs = 100L
        var waited = 0L

        while (waited < maxWaitMs) {
            val lspServerManager = LspServerManager.getInstance(project)
            val servers = lspServerManager.getServersForProvider(KnipLspServerSupportProvider::class.java)

            // Check if any server is running
            val runningServer = servers.find { server ->
                server.state == LspServerState.Running
            }

            if (runningServer != null) {
                LOG.info("Knip LSP server is ready after ${waited}ms, sending knip.start")
                sendKnipStartCommand(runningServer)
                return
            }

            delay(pollIntervalMs)
            waited += pollIntervalMs
        }

        LOG.warn("Timeout waiting for Knip LSP server to be ready after ${maxWaitMs}ms")
    }

    /**
     * Sends the knip.start command using the standard LSP workspace/executeCommand.
     */
    private suspend fun sendKnipStartCommand(server: LspServer) {
        try {
            val params = ExecuteCommandParams(KnipLspServerDescriptor.COMMAND_START, emptyList())
            server.sendRequest { lspServer ->
                lspServer.workspaceService.executeCommand(params)
            }
            LOG.info("Knip session started successfully")
        } catch (e: Exception) {
            LOG.warn("Failed to start Knip session: ${e.message}", e)
        }
    }

    /**
     * Clears the startup state for the given project.
     * Call this when restarting the server.
     */
    fun clearStartupState(project: Project) {
        val projectPath = project.basePath ?: project.name
        startupInProgress.remove(projectPath)
    }
}
