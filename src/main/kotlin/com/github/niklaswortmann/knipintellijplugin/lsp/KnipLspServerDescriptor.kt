package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.lang.typescript.lsp.JSFrameworkLspServerDescriptor
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.ConfigurationItem

/**
 * LSP Server Descriptor for the Knip language server.
 * Extends JSFrameworkLspServerDescriptor to leverage the TypeScript/JavaScript
 * framework infrastructure for package resolution via KnipLspServerActivationRule.
 */
internal class KnipLspServerDescriptor(project: Project) : JSFrameworkLspServerDescriptor(project, KnipLspServerActivationRule, "Knip") {

    /**
     * Creates initialization options for the TypeScript SDK path.
     */
    override fun createInitializationOptionsWithTS(targetPath: String): Any {
        @Suppress("unused")
        return object {
            val typescript = object {
                val tsdk = targetPath
            }
        }
    }

    /**
     * Provides workspace configuration for the Knip language server.
     * The server requests configuration via workspace/configuration with section 'knip'.
     */
    override fun getWorkspaceConfiguration(item: ConfigurationItem): Any? {
        if (item.section == "knip") {
            return KnipWorkspaceConfiguration()
        }
        return super.getWorkspaceConfiguration(item)
    }

    /**
     * Configuration object returned for workspace/configuration requests.
     * The Knip language server expects these settings.
     */
    @Suppress("unused") // Properties are serialized to JSON
    class KnipWorkspaceConfiguration {
        val enabled: Boolean = true

        val editor = EditorConfig()

        val imports = ImportsConfig()

        val exports = ExportsConfig()

        class EditorConfig {
            val exports = EditorExportsConfig()
        }

        class EditorExportsConfig {
            val codelens = CodeLensConfig()
            val hover = HoverConfig()
            val quickfix = QuickFixConfig()
            val highlight = HighlightConfig()
        }

        class CodeLensConfig {
            val enabled: Boolean = true
        }

        class HoverConfig {
            val enabled: Boolean = true
            val includeImportLocationSnippet: Boolean = true
            val maxSnippets: Int = 5
            val timeout: Int = 5000
        }

        class QuickFixConfig {
            val enabled: Boolean = true
        }

        class HighlightConfig {
            val dimExports: Boolean = false
            val dimTypes: Boolean = false
        }

        class ImportsConfig {
            val enabled: Boolean = true
        }

        class ExportsConfig {
            val enabled: Boolean = true
            val contention = ContentionConfig()
        }

        class ContentionConfig {
            val enabled: Boolean = true
        }
    }

    companion object {
        // Custom Knip LSP command names (sent via workspace/executeCommand)
        const val COMMAND_START = "knip.start"
        const val COMMAND_STOP = "knip.stop"
        const val COMMAND_RESTART = "knip.restart"
    }
}
