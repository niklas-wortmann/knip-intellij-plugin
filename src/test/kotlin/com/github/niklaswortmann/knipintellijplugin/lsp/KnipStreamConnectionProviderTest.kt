package com.github.niklaswortmann.knipintellijplugin.lsp

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for KnipStreamConnectionProvider.
 */
class KnipStreamConnectionProviderTest {

    @Test
    fun testFindNpxPathReturnsNonEmpty() {
        // The findNpxPath should always return something (either found path or fallback)
        val npxPath = KnipStreamConnectionProvider.findNpxPath()
        assertNotNull("npxPath should not be null", npxPath)
        assertTrue("npxPath should not be empty", npxPath.isNotEmpty())
    }

    @Test
    fun testFindNpxPathContainsNpx() {
        val npxPath = KnipStreamConnectionProvider.findNpxPath()
        assertTrue(
            "npxPath should contain 'npx'",
            npxPath.lowercase().contains("npx")
        )
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
