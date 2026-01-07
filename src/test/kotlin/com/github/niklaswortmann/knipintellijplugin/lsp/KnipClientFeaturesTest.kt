package com.github.niklaswortmann.knipintellijplugin.lsp

import com.redhat.devtools.lsp4ij.ServerStatus
import org.eclipse.lsp4j.jsonrpc.Endpoint
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Unit tests for KnipClientFeatures.
 */
class KnipClientFeaturesTest {

    @Test
    fun testHandleServerStatusChangedToStarting() {
        val features = KnipClientFeatures()

        // Should handle status change without errors
        // Note: Progress indicator won't show as serverWrapper is null in tests
        features.handleServerStatusChanged(ServerStatus.starting)

        // No exception means test passes
        assertTrue(true)
    }

    @Test
    fun testHandleServerStatusChangedToStarted() {
        val features = KnipClientFeatures()

        // Should handle status change without errors
        // Note: This will not actually start a session as serverWrapper is null in tests
        features.handleServerStatusChanged(ServerStatus.started)

        // No exception means test passes
        assertTrue(true)
    }

    @Test
    fun testHandleServerStatusChangedToStopped() {
        val features = KnipClientFeatures()

        // Should handle status change without errors
        features.handleServerStatusChanged(ServerStatus.stopped)

        // No exception means test passes
        assertTrue(true)
    }

    @Test
    fun testHandleServerStatusChangedToStopping() {
        val features = KnipClientFeatures()

        // Should handle status change without errors
        features.handleServerStatusChanged(ServerStatus.stopping)

        // No exception means test passes
        assertTrue(true)
    }

    @Test
    fun testHandleServerStatusChangedSequence() {
        val features = KnipClientFeatures()

        // Simulate a typical server lifecycle: starting -> started -> stopping -> stopped
        // All transitions should be handled gracefully without serverWrapper
        features.handleServerStatusChanged(ServerStatus.starting)
        features.handleServerStatusChanged(ServerStatus.started)
        features.handleServerStatusChanged(ServerStatus.stopping)
        features.handleServerStatusChanged(ServerStatus.stopped)

        // No exception means test passes
        assertTrue(true)
    }

    @Test
    fun testExtractEndpointReturnsNullForNonProxyObject() {
        val features = KnipClientFeatures()

        // Access the private method using reflection
        val method = KnipClientFeatures::class.java.getDeclaredMethod("extractEndpoint", Any::class.java)
        method.isAccessible = true

        val result = method.invoke(features, "not a proxy")

        assertNull("Should return null for non-proxy objects", result)
    }

    @Test
    fun testExtractEndpointReturnsEndpointDirectly() {
        val features = KnipClientFeatures()

        // Create a mock endpoint
        val mockEndpoint = object : Endpoint {
            override fun request(method: String?, parameter: Any?) = null
            override fun notify(method: String?, parameter: Any?) {}
        }

        // Access the private method using reflection
        val method = KnipClientFeatures::class.java.getDeclaredMethod("extractEndpoint", Any::class.java)
        method.isAccessible = true

        val result = method.invoke(features, mockEndpoint) as? Endpoint

        assertNotNull("Should return the endpoint directly", result)
        assertSame("Should return the same endpoint instance", mockEndpoint, result)
    }

    @Test
    fun testExtractEndpointHandlesInvalidProxy() {
        val features = KnipClientFeatures()

        // Create a proxy without delegate field
        val proxy = Proxy.newProxyInstance(
            this.javaClass.classLoader,
            arrayOf(Runnable::class.java)
        ) { _, _, _ -> null }

        // Access the private method using reflection
        val method = KnipClientFeatures::class.java.getDeclaredMethod("extractEndpoint", Any::class.java)
        method.isAccessible = true

        val result = method.invoke(features, proxy)

        // Should handle gracefully and return null
        assertNull("Should return null for invalid proxy", result)
    }

    @Test
    fun testCompanionConstants() {
        assertEquals("knip.start", KnipClientFeatures.REQUEST_START)
        assertEquals("knip.stop", KnipClientFeatures.REQUEST_STOP)
        assertEquals("knip.restart", KnipClientFeatures.REQUEST_RESTART)
    }
}
