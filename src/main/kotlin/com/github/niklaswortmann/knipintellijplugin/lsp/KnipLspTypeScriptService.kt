package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.lang.typescript.lsp.JSFrameworkLspTypeScriptService
import com.intellij.openapi.project.Project

/**
 * LSP-based TypeScript service implementation for Knip.
 * Extends JSFrameworkLspTypeScriptService to integrate with IntelliJ's TypeScript service infrastructure.
 */
class KnipLspTypeScriptService(
    project: Project,
) : JSFrameworkLspTypeScriptService(
    project = project,
    providerClass = KnipLspServerSupportProvider::class.java,
    activationRule = KnipLspServerActivationRule,
) {
    override val name: String
        get() = "Knip"

    override val prefix: String
        get() = "Knip"
}
