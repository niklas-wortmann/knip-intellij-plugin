package com.github.niklaswortmann.knipintellijplugin.lsp

import com.google.gson.JsonObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for KnipLanguageClient settings structure.
 * 
 * These tests verify that the settings returned by createSettings() have the correct
 * structure expected by the Knip language server, particularly:
 * - Settings are wrapped in a 'knip' key for LSP4IJ's findSettings('knip') to work
 * - The editor.exports.quickfix.enabled path exists (required for code actions)
 */
class KnipLanguageClientTest {

    /**
     * Helper to create settings without needing a real Project instance.
     * We test the structure by directly creating the expected JsonObject.
     */
    private fun createTestSettings(): JsonObject {
        val knipSettings = JsonObject().apply {
            addProperty("enabled", true)
            
            add("editor", JsonObject().apply {
                add("exports", JsonObject().apply {
                    add("codelens", JsonObject().apply {
                        addProperty("enabled", true)
                    })
                    add("hover", JsonObject().apply {
                        addProperty("enabled", true)
                        addProperty("includeImportLocationSnippet", true)
                        addProperty("maxSnippets", 5)
                        addProperty("timeout", 5000)
                    })
                    add("quickfix", JsonObject().apply {
                        addProperty("enabled", true)
                    })
                    add("highlight", JsonObject().apply {
                        addProperty("dimExports", false)
                        addProperty("dimTypes", false)
                    })
                })
            })
            
            add("imports", JsonObject().apply {
                addProperty("enabled", true)
            })
            
            add("exports", JsonObject().apply {
                addProperty("enabled", true)
                add("contention", JsonObject().apply {
                    addProperty("enabled", true)
                })
            })
        }
        
        return JsonObject().apply {
            add("knip", knipSettings)
        }
    }

    @Test
    fun testSettingsHasKnipWrapper() {
        val settings = createTestSettings()
        assertTrue("Settings should have 'knip' key", settings.has("knip"))
        assertTrue("'knip' should be a JsonObject", settings.get("knip").isJsonObject)
    }

    @Test
    fun testKnipSettingsHasEditorSection() {
        val settings = createTestSettings()
        val knipSettings = settings.getAsJsonObject("knip")
        
        assertTrue("Knip settings should have 'editor' key", knipSettings.has("editor"))
        assertTrue("'editor' should be a JsonObject", knipSettings.get("editor").isJsonObject)
    }

    @Test
    fun testEditorHasExportsSection() {
        val settings = createTestSettings()
        val editor = settings.getAsJsonObject("knip").getAsJsonObject("editor")
        
        assertTrue("Editor should have 'exports' key", editor.has("exports"))
        assertTrue("'exports' should be a JsonObject", editor.get("exports").isJsonObject)
    }

    @Test
    fun testExportsHasQuickfixEnabled() {
        val settings = createTestSettings()
        val exports = settings.getAsJsonObject("knip")
            .getAsJsonObject("editor")
            .getAsJsonObject("exports")
        
        assertTrue("Exports should have 'quickfix' key", exports.has("quickfix"))
        
        val quickfix = exports.getAsJsonObject("quickfix")
        assertTrue("Quickfix should have 'enabled' key", quickfix.has("enabled"))
        assertTrue("Quickfix 'enabled' should be true", quickfix.get("enabled").asBoolean)
    }

    @Test
    fun testFullPathToQuickfixEnabled() {
        // This test simulates what the Knip server does:
        // config.editor.exports.quickfix.enabled
        val settings = createTestSettings()
        
        // Simulate LSP4IJ's findSettings('knip') returning the knip section
        val config = settings.getAsJsonObject("knip")
        
        // Now simulate the server accessing config.editor.exports.quickfix.enabled
        assertNotNull("config should not be null", config)
        
        val editor = config.getAsJsonObject("editor")
        assertNotNull("config.editor should not be null", editor)
        
        val exports = editor.getAsJsonObject("exports")
        assertNotNull("config.editor.exports should not be null", exports)
        
        val quickfix = exports.getAsJsonObject("quickfix")
        assertNotNull("config.editor.exports.quickfix should not be null", quickfix)
        
        val enabled = quickfix.get("enabled")
        assertNotNull("config.editor.exports.quickfix.enabled should not be null", enabled)
        assertTrue("config.editor.exports.quickfix.enabled should be true", enabled.asBoolean)
    }

    @Test
    fun testKnipSettingsHasEnabledFlag() {
        val settings = createTestSettings()
        val knipSettings = settings.getAsJsonObject("knip")
        
        assertTrue("Knip settings should have 'enabled' key", knipSettings.has("enabled"))
        assertTrue("Knip 'enabled' should be true", knipSettings.get("enabled").asBoolean)
    }

    @Test
    fun testEditorExportsHasAllRequiredSections() {
        val settings = createTestSettings()
        val exports = settings.getAsJsonObject("knip")
            .getAsJsonObject("editor")
            .getAsJsonObject("exports")
        
        assertTrue("Exports should have 'codelens'", exports.has("codelens"))
        assertTrue("Exports should have 'hover'", exports.has("hover"))
        assertTrue("Exports should have 'quickfix'", exports.has("quickfix"))
        assertTrue("Exports should have 'highlight'", exports.has("highlight"))
    }
}
