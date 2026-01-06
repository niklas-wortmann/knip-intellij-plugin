package com.github.niklaswortmann.knipintellijplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.redhat.devtools.lsp4ij.LanguageServerManager

/**
 * Action to restart the Knip language server.
 * Available in Tools menu and can be triggered via Find Action.
 */
class RestartKnipServerAction : AnAction("Restart Knip Language Server") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Use LSP4IJ's LanguageServerManager to restart the server
        LanguageServerManager.getInstance(project).stop("knipLanguageServer")
        LanguageServerManager.getInstance(project).start("knipLanguageServer")
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
