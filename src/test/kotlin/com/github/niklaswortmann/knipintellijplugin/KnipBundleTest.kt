package com.github.niklaswortmann.knipintellijplugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for KnipBundle message resources.
 */
class KnipBundleTest {

    @Test
    fun testBundleMessages() {
        assertEquals("Knip", KnipBundle.message("pluginName"))
        assertEquals(
            "Find unused files, dependencies and exports in JavaScript/TypeScript projects",
            KnipBundle.message("pluginDescription")
        )
        assertEquals("Knip language server started", KnipBundle.message("serverStarted"))
        assertEquals("Knip language server stopped", KnipBundle.message("serverStopped"))
        assertEquals("Knip", KnipBundle.message("settingsTitle"))
        assertEquals("Node.js path", KnipBundle.message("settingsNodePath"))
        assertEquals("Enable Knip", KnipBundle.message("settingsEnabled"))
    }

    @Test
    fun testServerErrorMessage() {
        val errorMsg = "Connection refused"
        val formatted = KnipBundle.message("serverError", errorMsg)
        assertTrue("Error message should contain the error", formatted.contains(errorMsg))
    }
}
