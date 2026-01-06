package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider

/**
 * Factory for creating the Knip language server connection.
 * This factory is registered via the LSP4IJ extension point and is responsible
 * for spawning the Knip language server process.
 */
class KnipLanguageServerFactory : LanguageServerFactory {

    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return KnipStreamConnectionProvider(project)
    }

    override fun createLanguageClient(project: Project): LanguageClientImpl {
        return KnipLanguageClient(project)
    }

    override fun createClientFeatures(): LSPClientFeatures {
        return KnipClientFeatures()
    }
}
