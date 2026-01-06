package com.github.niklaswortmann.knipintellijplugin.lsp

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for KnipStreamConnectionProvider.
 */
class KnipStreamConnectionProviderTest {

    @Test
    fun testFindNodePathReturnsNonEmpty() {
        // The findNodePath should always return something (either found path or fallback)
        val nodePath = KnipStreamConnectionProvider.findNodePath()
        assertNotNull("nodePath should not be null", nodePath)
        assertTrue("nodePath should not be empty", nodePath.isNotEmpty())
    }

    @Test
    fun testFindNodePathContainsNode() {
        val nodePath = KnipStreamConnectionProvider.findNodePath()
        assertTrue(
            "nodePath should contain 'node'",
            nodePath.lowercase().contains("node")
        )
    }

    @Test
    fun testFindLanguageServerPathReturnsNullOrValidPath() {
        // The findLanguageServerPath may return null if not installed, or a valid path
        val lsPath = KnipStreamConnectionProvider.findLanguageServerPath(null)
        if (lsPath != null) {
            assertTrue("Language server path should end with index.js", lsPath.endsWith("index.js"))
        }
        // null is also acceptable if the package is not installed
    }

    @Test
    fun testIsWindowsDetection() {
        // This test verifies the OS detection doesn't throw
        val osName = System.getProperty("os.name").lowercase()
        val isWindows = osName.contains("windows")
        
        // Just verify we can detect the OS without errors
        assertNotNull(osName)
    }
}
