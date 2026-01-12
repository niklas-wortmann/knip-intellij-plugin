package com.github.niklaswortmann.knipintellijplugin.settings

import com.github.niklaswortmann.knipintellijplugin.lsp.KnipLspServerLoader
import com.github.niklaswortmann.knipintellijplugin.lsp.KnipLspServerSupportProvider
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.lang.typescript.lsp.createPackageRef
import com.intellij.lang.typescript.lsp.defaultPackageKey
import com.intellij.lang.typescript.lsp.extractRefText
import com.intellij.lang.typescript.lsp.restartTypeScriptServicesAsync
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerManager

fun getKnipSettings(project: Project): KnipSettings = project.service<KnipSettings>()

/**
 * Service mode for the Knip language server.
 */
enum class KnipServiceMode {
    ENABLED,
    DISABLED
}

/**
 * Persistent settings for the Knip plugin.
 * Settings are stored per-project in the workspace file.
 */
@Service(Service.Level.PROJECT)
@State(name = "KnipSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class KnipSettings(val project: Project) : SimplePersistentStateComponent<KnipSettingsState>(KnipSettingsState()) {

    var serviceMode: KnipServiceMode
        get() = state.innerServiceMode
        set(value) {
            val changed = state.innerServiceMode != value
            state.innerServiceMode = value
            if (changed) {
                restartTypeScriptServicesAsync(project)
                LspServerManager.getInstance(project).stopAndRestartIfNeeded(KnipLspServerSupportProvider::class.java)
            }
        }

    var lspServerPackageRef: NodePackageRef
        get() = createPackageRef(state.lspServerPackageName, KnipLspServerLoader.packageDescriptor.serverPackage)
        set(value) {
            val refText = extractRefText(value)
            val changed = state.lspServerPackageName != refText
            state.lspServerPackageName = refText
            if (changed) {
                restartTypeScriptServicesAsync(project)
                LspServerManager.getInstance(project).stopAndRestartIfNeeded(KnipLspServerSupportProvider::class.java)
            }
        }

    var serverArguments: String
        get() = state.serverArguments ?: "--stdio"
        set(value) {
            state.serverArguments = value
        }

    // Legacy compatibility properties
    var enabled: Boolean
        get() = serviceMode == KnipServiceMode.ENABLED
        set(value) { serviceMode = if (value) KnipServiceMode.ENABLED else KnipServiceMode.DISABLED }

    var nodePath: String
        get() = state.nodePath ?: ""
        set(value) { state.nodePath = value }

    var languageServerPath: String
        get() = state.languageServerPath ?: ""
        set(value) { state.languageServerPath = value }

    companion object {
        fun getInstance(project: Project): KnipSettings = getKnipSettings(project)
    }
}

class KnipSettingsState : BaseState() {
    var innerServiceMode: KnipServiceMode by enum(KnipServiceMode.ENABLED)
    var lspServerPackageName: String? by string(defaultPackageKey)
    var serverArguments: String? by string("--stdio")
    // Legacy fields for backward compatibility
    var nodePath: String? by string()
    var languageServerPath: String? by string()
}
