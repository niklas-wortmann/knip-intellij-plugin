package com.github.niklaswortmann.knipintellijplugin.lsp

import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.Lsp4jClient
import com.intellij.platform.lsp.api.LspServerNotificationsHandler
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCustomization
import com.intellij.platform.lsp.api.customization.LspDiagnosticsSupport
import org.eclipse.lsp4j.ConfigurationItem
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * LSP Server Descriptor for the Knip language server.
 * Configures how the server is started and which files it supports.
 */
class KnipLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Knip") {

    companion object {
        private val LOG = Logger.getInstance(KnipLspServerDescriptor::class.java)

        // Map to track module graph completion per project
        private val moduleGraphBuiltFutures = ConcurrentHashMap<String, CompletableFuture<Unit>>()

        /**
         * Gets or creates a CompletableFuture that completes when knip.moduleGraphBuilt is received.
         */
        fun getModuleGraphBuiltFuture(projectPath: String): CompletableFuture<Unit> {
            return moduleGraphBuiltFutures.computeIfAbsent(projectPath) {
                CompletableFuture()
            }
        }

        /**
         * Resets the module graph built future for a project (e.g., on server restart).
         */
        fun resetModuleGraphBuiltFuture(projectPath: String) {
            moduleGraphBuiltFutures.remove(projectPath)
        }

        /**
         * Called when the knip.moduleGraphBuilt notification is received.
         */
        internal fun onModuleGraphBuilt(projectPath: String) {
            LOG.info("Received knip.moduleGraphBuilt notification for project: $projectPath")
            val future = moduleGraphBuiltFutures[projectPath]
            if (future != null && !future.isDone) {
                future.complete(Unit)
            }
        }
    }

    override fun createLsp4jClient(handler: LspServerNotificationsHandler): Lsp4jClient {
        return KnipLsp4jClient(handler, project.basePath ?: "")
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
            val detectedPath = KnipNodeResolver.findLanguageServerPath(project.basePath)
            if (detectedPath != null) {
                LOG.info("Found @knip/language-server at: $detectedPath")
            }
            detectedPath
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
     * Customize LSP behavior for the Knip language server.
     *
     * The Knip language server uses push-based diagnostics (textDocument/publishDiagnostics)
     * and does not support pull diagnostics (textDocument/diagnostic).
     *
     * IntelliJ 2025.2+ enables pull diagnostics by default, which can cause the server
     * to not send push diagnostics if it sees the diagnostic capability.
     * We disable pull diagnostics by returning false from shouldAskServerForDiagnostics
     * to ensure only push diagnostics are used.
     */
    override val lspCustomization: LspCustomization = KnipLspCustomization()

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
 * The Knip language server uses "knip.start" to trigger analysis.
 */
interface KnipLanguageServer : LanguageServer {

    @JsonRequest("knip.start")
    fun knipStart(): CompletableFuture<Any?>
}

/**
 * Custom LSP4J client that handles Knip-specific notifications.
 * Extends the base Lsp4jClient to receive the knip.moduleGraphBuilt notification.
 */
class KnipLsp4jClient(
    handler: LspServerNotificationsHandler,
    private val projectPath: String
) : Lsp4jClient(handler) {

    /**
     * Handles the knip.moduleGraphBuilt notification from the language server.
     * This notification is sent when Knip has finished building the module graph
     * and is ready to provide diagnostics.
     */
    @JsonNotification("knip.moduleGraphBuilt")
    fun moduleGraphBuilt() {
        KnipLspServerDescriptor.onModuleGraphBuilt(projectPath)
    }
}

/**
 * Custom LSP customization for Knip that disables pull diagnostics.
 * The Knip language server only supports push-based diagnostics.
 */
private class KnipLspCustomization : LspCustomization() {
    override val diagnosticsCustomizer = KnipDiagnosticsCustomizer()
}

/**
 * Custom diagnostics customizer that disables pull diagnostics.
 * Returns false for shouldAskServerForDiagnostics to prevent IntelliJ
 * from sending textDocument/diagnostic requests.
 */
private class KnipDiagnosticsCustomizer : LspDiagnosticsSupport() {
    override fun shouldAskServerForDiagnostics(file: VirtualFile): Boolean = false
}
