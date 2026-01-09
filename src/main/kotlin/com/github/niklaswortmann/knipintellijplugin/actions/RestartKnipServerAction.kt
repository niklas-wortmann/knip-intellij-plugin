package com.github.niklaswortmann.knipintellijplugin.actions

import com.github.niklaswortmann.knipintellijplugin.KnipBundle
import com.github.niklaswortmann.knipintellijplugin.lsp.KnipLspServerSupportProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Action to restart the Knip language server.
 * Available in Tools menu and can be triggered via Find Action.
 */
class RestartKnipServerAction : AnAction(KnipBundle.message("actionRestartServer")) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Use Platform LSP API to restart the server
        KnipLspServerSupportProvider.restartServerAsync(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
