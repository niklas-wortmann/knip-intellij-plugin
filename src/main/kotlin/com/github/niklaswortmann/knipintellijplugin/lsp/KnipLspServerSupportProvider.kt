package com.github.niklaswortmann.knipintellijplugin.lsp

import com.github.niklaswortmann.knipintellijplugin.KnipIcons
import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettingsConfigurable
import com.intellij.lang.typescript.lsp.JSFrameworkLspServerDescriptor
import com.intellij.lang.typescript.lsp.JSFrameworkLspServerSupportProvider
import com.intellij.lang.typescript.lsp.JSLspServerWidgetItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem

/**
 * LSP Server Support Provider for the Knip language server.
 * Extends JSFrameworkLspServerSupportProvider to leverage the TypeScript/JavaScript
 * framework infrastructure for package resolution and activation rules.
 */
internal class KnipLspServerSupportProvider : JSFrameworkLspServerSupportProvider(KnipLspServerActivationRule) {

    override fun createLspServerDescriptor(project: Project): JSFrameworkLspServerDescriptor {
        // Initiate the startup sequence to send knip.start when server is ready
        KnipServerStartupHandler.initiateStartup(project)
        return KnipLspServerDescriptor(project)
    }

    override fun createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?): LspServerWidgetItem =
        JSLspServerWidgetItem(lspServer, currentFile, KnipIcons.Knip, KnipIcons.Knip, KnipSettingsConfigurable::class.java)
}
