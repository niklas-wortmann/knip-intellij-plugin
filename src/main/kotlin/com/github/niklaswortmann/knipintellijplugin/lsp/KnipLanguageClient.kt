package com.github.niklaswortmann.knipintellijplugin.lsp

import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl

/**
 * Custom language client for the Knip language server.
 * Provides the required configuration settings that the Knip language server expects.
 * 
 * The Knip language server requests configuration via workspace/configuration with section 'knip'.
 * LSP4IJ's findSettings() looks for a key matching the section name in the JsonObject returned
 * by createSettings(), so we must wrap our settings inside a 'knip' key.
 */
class KnipLanguageClient(project: Project) : LanguageClientImpl(project) {

    /**
     * Creates the settings object that will be returned for workspace/configuration requests.
     * 
     * The Knip language server calls `connection.workspace.getConfiguration('knip')` which triggers
     * an LSP workspace/configuration request with section='knip'. LSP4IJ's findSettings() method
     * looks for a 'knip' key in this JsonObject, so we must structure our settings accordingly.
     */
    override fun createSettings(): Any {
        val knipSettings = JsonObject().apply {
            addProperty("enabled", true)
            
            // Editor settings required by the Knip language server for code actions
            // The server accesses config.editor.exports.quickfix.enabled without null checks
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
            
            // Imports settings
            add("imports", JsonObject().apply {
                addProperty("enabled", true)
            })
            
            // Exports settings
            add("exports", JsonObject().apply {
                addProperty("enabled", true)
                add("contention", JsonObject().apply {
                    addProperty("enabled", true)
                })
            })
        }
        
        // Wrap settings in 'knip' key so LSP4IJ's findSettings('knip') can find them
        return JsonObject().apply {
            add("knip", knipSettings)
        }
    }
}
