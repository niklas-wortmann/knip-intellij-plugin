package com.github.niklaswortmann.knipintellijplugin.actions

import com.github.niklaswortmann.knipintellijplugin.KnipBundle
import com.github.niklaswortmann.knipintellijplugin.lsp.KnipLspServerSupportProvider
import com.github.niklaswortmann.knipintellijplugin.lsp.KnipServerStartupHandler
import com.intellij.lang.typescript.lsp.restartTypeScriptServicesAsync
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.platform.lsp.api.LspServerManager

/**
 * Action to restart the Knip language server.
 * Available in Tools menu and can be triggered via Find Action.
 */
class RestartKnipServerAction : AnAction(KnipBundle.message("actionRestartServer")) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Clear the startup state to allow re-initialization after restart
        KnipServerStartupHandler.clearStartupState(project)

        // Restart TypeScript services to refresh the language service provider
        restartTypeScriptServicesAsync(project)

        // Also restart the LSP server
        LspServerManager.getInstance(project)
            .stopAndRestartIfNeeded(KnipLspServerSupportProvider::class.java)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
