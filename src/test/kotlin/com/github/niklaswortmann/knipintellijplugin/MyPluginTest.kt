package com.github.niklaswortmann.knipintellijplugin

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testBundleMessages() {
        // Verify bundle messages are accessible
        assertEquals("Knip", MyBundle.message("pluginName"))
        assertEquals("Find unused files, dependencies and exports in JavaScript/TypeScript projects", 
            MyBundle.message("pluginDescription"))
        assertEquals("Knip language server started", MyBundle.message("serverStarted"))
        assertEquals("Knip language server stopped", MyBundle.message("serverStopped"))
        assertEquals("Knip", MyBundle.message("settingsTitle"))
        assertEquals("Node.js path", MyBundle.message("settingsNodePath"))
        assertEquals("Enable Knip", MyBundle.message("settingsEnabled"))
    }

    fun testServerErrorMessage() {
        val errorMsg = "Connection refused"
        val formatted = MyBundle.message("serverError", errorMsg)
        assertTrue("Error message should contain the error", formatted.contains(errorMsg))
    }

    override fun getTestDataPath() = "src/test/testData"
}
