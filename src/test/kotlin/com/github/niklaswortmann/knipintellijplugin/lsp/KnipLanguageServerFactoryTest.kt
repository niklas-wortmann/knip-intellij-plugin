package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.*

/**
 * Unit tests for KnipLanguageServerFactory.
 */
class KnipLanguageServerFactoryTest : BasePlatformTestCase() {

    fun testCreateConnectionProvider() {
        val factory = KnipLanguageServerFactory()
        val provider = factory.createConnectionProvider(project)

        assertNotNull("Connection provider should not be null", provider)
        assertTrue(
            "Should create KnipStreamConnectionProvider",
            provider is KnipStreamConnectionProvider
        )
    }

    fun testCreateLanguageClient() {
        val factory = KnipLanguageServerFactory()
        val client = factory.createLanguageClient(project)

        assertNotNull("Language client should not be null", client)
        assertTrue(
            "Should create KnipLanguageClient",
            client is KnipLanguageClient
        )
    }

    fun testCreateClientFeatures() {
        val factory = KnipLanguageServerFactory()
        val features = factory.createClientFeatures()

        assertNotNull("Client features should not be null", features)
        assertTrue(
            "Should create KnipClientFeatures",
            features is KnipClientFeatures
        )
    }
}
