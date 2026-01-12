package com.github.niklaswortmann.knipintellijplugin.lsp

import com.github.niklaswortmann.knipintellijplugin.settings.getKnipSettings
import com.github.niklaswortmann.knipintellijplugin.settings.KnipServiceMode
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.lang.typescript.lsp.LspServerActivationRule
import com.intellij.lang.typescript.lsp.LspServerLoader
import com.intellij.lang.typescript.lsp.LspServerPackageDescriptor
import com.intellij.lang.typescript.lsp.PackageVersion
import com.intellij.lang.typescript.lsp.ServiceActivationHelper
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus

/**
 * Default version for the @knip/language-server package.
 * Uses bundled version from the plugin's resources.
 */
private val knipLspServerPackageVersion = PackageVersion.bundled<KnipLspServerPackageDescriptor>(
    version = "1.0.0",
    pluginPath = "knip-intellij-plugin",
    localPath = "language-server",
    isBundledEnabled = { Registry.`is`("knip.language.server.bundled.enabled", true) }
)

/**
 * Package descriptor for the @knip/language-server npm package.
 * Defines the package name, default version, and entry point path.
 */
private object KnipLspServerPackageDescriptor : LspServerPackageDescriptor(
    name = "@knip/language-server",
    defaultVersion = knipLspServerPackageVersion,
    defaultPackageRelativePath = "/src/index.js"
) {
    override val registryVersion: String get() = Registry.stringValue("knip.language.server.default.version")
}

/**
 * Loader for the Knip language server package.
 * Handles package resolution using NodePackageRef from project settings.
 */
@ApiStatus.Experimental
object KnipLspServerLoader : LspServerLoader(KnipLspServerPackageDescriptor) {
    override fun getSelectedPackageRef(project: Project): NodePackageRef {
        return getKnipSettings(project).lspServerPackageRef
    }
}

/**
 * Activation rule for the Knip language server.
 * Determines when the server should be activated based on file type and project settings.
 */
object KnipLspServerActivationRule : LspServerActivationRule(KnipLspServerLoader, KnipActivationHelper) {
    override fun isFileAcceptable(file: VirtualFile): Boolean {
        return KnipFileSupport.isSupportedFile(file)
    }
}

/**
 * Shared constants and utility functions for the Knip language server.
 */
object KnipFileSupport {
    /**
     * Supported file extensions for the Knip language server.
     */
    val SUPPORTED_EXTENSIONS = setOf(
        "js", "jsx", "ts", "tsx", "mjs", "cjs", "mts", "cts"
    )

    /**
     * Supported file names (exact match).
     */
    val SUPPORTED_FILE_NAMES = setOf(
        "package.json", "knip.json", "knip.jsonc"
    )

    /**
     * Checks if the given file is supported by the Knip language server.
     */
    fun isSupportedFile(file: VirtualFile): Boolean {
        val fileName = file.name
        val extension = file.extension?.lowercase()

        // Check exact file names
        if (fileName in SUPPORTED_FILE_NAMES) {
            return true
        }

        // Check extensions
        if (extension in SUPPORTED_EXTENSIONS) {
            return true
        }

        // Check Knip-specific config file patterns
        if (fileName == "knip.ts" ||
            fileName == "knip.config.ts" ||
            fileName == "knip.config.js" ||
            fileName == "knip.config.mjs" ||
            fileName == "knip.config.cjs"
        ) {
            return true
        }

        return false
    }
}

/**
 * Helper object for checking Knip activation state.
 * Implements ServiceActivationHelper for integration with LspServerActivationRule.
 */
object KnipActivationHelper : ServiceActivationHelper {
    /**
     * Checks if Knip should be active for the given project and file context.
     */
    override fun isProjectContext(project: Project, context: VirtualFile): Boolean {
        return KnipFileSupport.isSupportedFile(context)
    }

    /**
     * Checks if Knip is enabled in the project settings.
     */
    override fun isEnabledInSettings(project: Project): Boolean {
        return getKnipSettings(project).serviceMode == KnipServiceMode.ENABLED
    }
}
