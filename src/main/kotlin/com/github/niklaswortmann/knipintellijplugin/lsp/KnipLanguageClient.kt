package com.github.niklaswortmann.knipintellijplugin.lsp

import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl

/**
 * Custom language client for the Knip language server.
 * Provides the required configuration settings that the Knip language server expects.
 */
class KnipLanguageClient(project: Project) : LanguageClientImpl(project) {

    /**
     * Creates the settings object that will be returned for workspace/configuration requests.
     * The Knip language server expects a configuration with the 'editor' section.
     */
    override fun createSettings(): Any {
        return JsonObject().apply {
            addProperty("enabled", true)
            
            // Editor settings required by the Knip language server
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
    }
}
