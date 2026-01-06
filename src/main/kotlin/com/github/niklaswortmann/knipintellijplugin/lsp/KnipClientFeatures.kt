package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.openapi.diagnostic.Logger
import com.redhat.devtools.lsp4ij.ServerStatus
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import org.eclipse.lsp4j.jsonrpc.Endpoint
import java.lang.reflect.Proxy

/**
 * Custom LSP client features for the Knip language server.
 * Configures how diagnostics and other LSP features are handled.
 * 
 * The Knip language server requires a custom "knip.start" request to be sent
 * after initialization to start the session and begin publishing diagnostics.
 * This is different from standard LSP servers that start automatically.
 */
class KnipClientFeatures : LSPClientFeatures() {
    
    companion object {
        private val LOG = Logger.getInstance(KnipClientFeatures::class.java)
        
        // Custom Knip LSP request methods
        const val REQUEST_START = "knip.start"
        const val REQUEST_STOP = "knip.stop"
        const val REQUEST_RESTART = "knip.restart"
    }
    
    private var knipSessionStarted = false
    
    /**
     * Called when the language server status changes.
     * When the server reaches "started" status, we send the custom "knip.start" request
     * to trigger the Knip session initialization and diagnostics publishing.
     */
    override fun handleServerStatusChanged(serverStatus: ServerStatus) {
        super.handleServerStatusChanged(serverStatus)
        
        if (serverStatus == ServerStatus.started && !knipSessionStarted) {
            startKnipSession()
        } else if (serverStatus == ServerStatus.stopped || serverStatus == ServerStatus.stopping) {
            knipSessionStarted = false
        }
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
                    }.exceptionally { error ->
                        LOG.warn("Failed to start Knip session: ${error.message}")
                        null
                    }
                } else {
                    LOG.warn("Could not extract Endpoint from language server proxy")
                }
            } catch (e: Exception) {
                LOG.warn("Failed to start Knip session: ${e.message}", e)
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
