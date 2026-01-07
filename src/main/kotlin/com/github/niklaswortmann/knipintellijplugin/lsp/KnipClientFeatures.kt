package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.ServerStatus
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import org.eclipse.lsp4j.jsonrpc.Endpoint
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Custom LSP client features for the Knip language server.
 * Configures how diagnostics and other LSP features are handled.
 * 
 * The Knip language server requires a custom "knip.start" request to be sent
 * after initialization to start the session and begin publishing diagnostics.
 * This is different from standard LSP servers that start automatically.
 * 
 * This class also manages progress visualization during server startup,
 * showing a progress indicator in the IDE status bar while Knip is analyzing the project.
 */
class KnipClientFeatures : LSPClientFeatures() {
    
    companion object {
        private val LOG = Logger.getInstance(KnipClientFeatures::class.java)
        
        // Custom Knip LSP request methods
        const val REQUEST_START = "knip.start"
        const val REQUEST_STOP = "knip.stop"
        const val REQUEST_RESTART = "knip.restart"
        
        // Progress messages
        private const val PROGRESS_TITLE = "Knip"
        private const val PROGRESS_STARTING = "Starting language server..."
        private const val PROGRESS_ANALYZING = "Analyzing project for unused code..."
    }
    
    private var knipSessionStarted = false
    private val progressIndicatorRef = AtomicReference<ProgressIndicator?>(null)
    private val analysisComplete = AtomicBoolean(false)
    private var analysisLatch: CountDownLatch? = null
    
    /**
     * Called when the language server status changes.
     * When the server reaches "started" status, we send the custom "knip.start" request
     * to trigger the Knip session initialization and diagnostics publishing.
     * 
     * Progress visualization:
     * - starting: Show "Starting language server..."
     * - started: Update to "Analyzing project..." and send knip.start
     * - stopped/stopping: Cancel progress indicator
     */
    override fun handleServerStatusChanged(serverStatus: ServerStatus) {
        super.handleServerStatusChanged(serverStatus)
        
        when (serverStatus) {
            ServerStatus.starting -> {
                showStartupProgress()
            }
            ServerStatus.started -> {
                if (!knipSessionStarted) {
                    updateProgressMessage(PROGRESS_ANALYZING)
                    startKnipSession()
                }
            }
            ServerStatus.stopped, ServerStatus.stopping -> {
                knipSessionStarted = false
                finishProgress()
            }
            else -> { /* ignore other states */ }
        }
    }
    
    /**
     * Shows a background progress indicator during Knip server startup.
     * The progress is shown in the IDE status bar and can be viewed in the Background Tasks popup.
     */
    private fun showStartupProgress() {
        val project = serverWrapper?.project ?: return
        
        // Reset state for new startup
        analysisComplete.set(false)
        analysisLatch = CountDownLatch(1)
        
        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(object : Task.Backgroundable(
                project,
                PROGRESS_TITLE,
                true  // cancellable
            ) {
                override fun run(indicator: ProgressIndicator) {
                    progressIndicatorRef.set(indicator)
                    indicator.isIndeterminate = true
                    indicator.text = PROGRESS_STARTING
                    
                    LOG.info("Knip startup progress indicator started")
                    
                    // Wait for analysis to complete or cancellation
                    try {
                        while (!analysisComplete.get() && !indicator.isCanceled) {
                            analysisLatch?.await(500, TimeUnit.MILLISECONDS)
                        }
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                    
                    progressIndicatorRef.set(null)
                    LOG.info("Knip startup progress indicator finished")
                }
                
                override fun onCancel() {
                    LOG.info("Knip startup progress was cancelled by user")
                    finishProgress()
                }
            })
        }
    }
    
    /**
     * Updates the progress indicator message.
     */
    private fun updateProgressMessage(message: String) {
        progressIndicatorRef.get()?.let { indicator ->
            indicator.text = message
        }
    }
    
    /**
     * Marks the progress as complete and releases the progress indicator.
     */
    private fun finishProgress() {
        analysisComplete.set(true)
        analysisLatch?.countDown()
    }
    
    /**
     * Sends the "knip.start" custom request to the language server.
     * This triggers the Knip session to start analyzing the project and publishing diagnostics.
     * 
     * The Knip language server uses custom JSON-RPC methods (not standard LSP).
     * We access the underlying Endpoint interface through the LSP4J proxy to send the request.
     */
    private fun startKnipSession() {
        val wrapper = serverWrapper ?: return
        
        LOG.info("Sending knip.start request to initialize Knip session")
        
        wrapper.initializedServer.thenAccept { server ->
            if (server == null) {
                LOG.warn("Language server is null, cannot start Knip session")
                finishProgress()
                return@thenAccept
            }
            
            try {
                // The LanguageServer returned by LSP4J is a Proxy that wraps an EndpointProxy
                // which in turn wraps an Endpoint. We need to extract the Endpoint to send
                // custom requests that aren't part of the standard LSP interface.
                val endpoint = extractEndpoint(server)
                if (endpoint != null) {
                    endpoint.request(REQUEST_START, null).thenAccept {
                        LOG.info("Knip session started successfully")
                        knipSessionStarted = true
                        finishProgress()
                    }.exceptionally { error ->
                        LOG.warn("Failed to start Knip session: ${error.message}")
                        finishProgress()
                        null
                    }
                } else {
                    LOG.warn("Could not extract Endpoint from language server proxy")
                    finishProgress()
                }
            } catch (e: Exception) {
                LOG.warn("Failed to start Knip session: ${e.message}", e)
                finishProgress()
            }
        }
    }
    
    /**
     * Extracts the Endpoint from an LSP4J language server proxy.
     * LSP4J creates proxies using EndpointProxy which holds a delegate Endpoint.
     */
    private fun extractEndpoint(server: Any): Endpoint? {
        return try {
            // Check if the server is a Proxy
            if (Proxy.isProxyClass(server.javaClass)) {
                val handler = Proxy.getInvocationHandler(server)
                // EndpointProxy has a 'delegate' field that holds the Endpoint
                val delegateField = handler.javaClass.getDeclaredField("delegate")
                delegateField.isAccessible = true
                delegateField.get(handler) as? Endpoint
            } else if (server is Endpoint) {
                server
            } else {
                null
            }
        } catch (e: Exception) {
            LOG.debug("Could not extract Endpoint: ${e.message}")
            null
        }
    }
}
