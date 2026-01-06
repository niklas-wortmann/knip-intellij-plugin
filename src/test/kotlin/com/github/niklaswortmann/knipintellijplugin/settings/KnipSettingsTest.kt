package com.github.niklaswortmann.knipintellijplugin.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for KnipSettings persistent state component.
 */
class KnipSettingsTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        // Reset settings to defaults before each test
        val settings = KnipSettings.getInstance(project)
        settings.loadState(KnipSettings.State())
    }

    fun testDefaultSettings() {
        val settings = KnipSettings.getInstance(project)
        
        // Verify default values
        assertTrue("Plugin should be enabled by default", settings.enabled)
        assertEquals("Node path should be empty by default", "", settings.nodePath)
        assertEquals("Npx path should be empty by default", "", settings.npxPath)
        assertEquals("Server arguments should default to --stdio", "--stdio", settings.serverArguments)
    }

    fun testSettingsModification() {
        val settings = KnipSettings.getInstance(project)
        
        // Modify settings
        settings.enabled = false
        settings.nodePath = "/custom/node"
        settings.npxPath = "/custom/npx"
        settings.serverArguments = "--stdio --verbose"
        
        // Verify modifications
        assertFalse("Plugin should be disabled", settings.enabled)
        assertEquals("/custom/node", settings.nodePath)
        assertEquals("/custom/npx", settings.npxPath)
        assertEquals("--stdio --verbose", settings.serverArguments)
    }

    fun testGetState() {
        val settings = KnipSettings.getInstance(project)
        settings.enabled = true
        settings.nodePath = "/test/node"
        
        val state = settings.state
        assertNotNull("State should not be null", state)
        assertTrue("State enabled should match", state.enabled)
        assertEquals("State nodePath should match", "/test/node", state.nodePath)
    }

    fun testLoadState() {
        val settings = KnipSettings.getInstance(project)
        
        val newState = KnipSettings.State(
            enabled = false,
            nodePath = "/loaded/node",
            npxPath = "/loaded/npx",
            serverArguments = "--custom-arg"
        )
        
        settings.loadState(newState)
        
        assertFalse("Loaded enabled should be false", settings.enabled)
        assertEquals("/loaded/node", settings.nodePath)
        assertEquals("/loaded/npx", settings.npxPath)
        assertEquals("--custom-arg", settings.serverArguments)
    }
}
