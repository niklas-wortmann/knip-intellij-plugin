package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.client.features.LSPCodeActionFeature

/**
 * Custom LSP client features for the Knip language server.
 * Configures how diagnostics and other LSP features are handled.
 */
class KnipClientFeatures : LSPClientFeatures() {
    init {
        // Disable code actions as the Knip language server doesn't properly support them
        // and throws "Cannot read properties of null (reading 'editor')" errors
        setCodeActionFeature(object : LSPCodeActionFeature() {
            override fun isSupported(file: PsiFile): Boolean = false
        })
    }
}
