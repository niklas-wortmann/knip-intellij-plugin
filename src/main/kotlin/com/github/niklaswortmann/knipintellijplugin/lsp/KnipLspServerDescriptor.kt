package com.github.niklaswortmann.knipintellijplugin.lsp

import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import org.eclipse.lsp4j.ConfigurationItem
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture

/**
 * LSP Server Descriptor for the Knip language server.
 * Configures how the server is started and which files it supports.
 */
class KnipLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Knip") {

    companion object {
        private val LOG = Logger.getInstance(KnipLspServerDescriptor::class.java)
        
        // Custom Knip LSP request methods
        const val REQUEST_START = "knip.start"
        const val REQUEST_STOP = "knip.stop"
        const val REQUEST_RESTART = "knip.restart"
    }

    override fun isSupportedFile(file: VirtualFile): Boolean {
        return KnipLspServerSupportProvider.isSupportedFile(file)
    }

    override fun createCommandLine(): GeneralCommandLine {
        val settings = KnipSettings.getInstance(project)

        // Use custom node path from settings, or auto-detect
        val nodePath = if (settings.nodePath.isNotBlank()) {
            LOG.info("Using custom node path from settings: ${settings.nodePath}")
            settings.nodePath
        } else {
            KnipNodeResolver.findNodePath()
        }

        // Use custom language server path from settings, or auto-detect
        val languageServerPath = if (settings.languageServerPath.isNotBlank()) {
            LOG.info("Using custom language server path from settings: ${settings.languageServerPath}")
            settings.languageServerPath
        } else {
            KnipNodeResolver.findLanguageServerPath(project.basePath)
        }

        if (languageServerPath == null) {
            LOG.warn("Language server path not found, using error command")
            // Return a command that will fail with a helpful error message
            return GeneralCommandLine(
                nodePath,
                "-e",
                "console.error('Error: @knip/language-server package not found. Please install it globally with: npm install -g @knip/language-server'); process.exit(1);"
            ).apply {
                project.basePath?.let { withWorkDirectory(it) }
            }
        }

        val commandLine = GeneralCommandLine().apply {
            exePath = nodePath
            addParameter(languageServerPath)

            // Add server arguments from settings
            val args = settings.serverArguments.split(" ")
                .filter { it.isNotBlank() }
            args.forEach { addParameter(it) }

            // Set working directory to project base path
            project.basePath?.let { withWorkDirectory(it) }
        }

        LOG.info("Starting Knip language server with command: ${commandLine.commandLineString}")
        return commandLine
    }

    /**
     * Override to use our custom server interface that supports the knip.start request.
     */
    override val lsp4jServerClass: Class<out LanguageServer> = KnipLanguageServer::class.java

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
}

/**
 * Extended LanguageServer interface that includes Knip-specific custom requests.
 * The Knip language server requires a "knip.start" request to be sent after initialization
 * to start the session and begin publishing diagnostics.
 */
interface KnipLanguageServer : LanguageServer {
    
    @JsonRequest("knip.start")
    fun knipStart(): CompletableFuture<Any?>
    
    @JsonRequest("knip.stop")
    fun knipStop(): CompletableFuture<Any?>
    
    @JsonRequest("knip.restart")
    fun knipRestart(): CompletableFuture<Any?>
}
