package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.lang.typescript.compiler.TypeScriptService
import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.lang.typescript.languageService.TypeScriptServiceProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile

/**
 * Language service provider for Knip.
 * Aggregates and manages the LSP-based TypeScript service instance.
 * This integrates Knip with IntelliJ's TypeScript service infrastructure.
 */
internal class KnipLanguageServiceProvider(project: Project) : TypeScriptServiceProvider() {
    private val lspLanguageService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        project.service<KnipLspServiceWrapper>()
    }

    override val allServices: List<TypeScriptService>
        get() = listOf(lspLanguageService.service)

    override fun isHighlightingCandidate(file: VirtualFile): Boolean {
        return TypeScriptLanguageServiceUtil.isJavaScriptOrTypeScriptFileType(file.fileType)
                || KnipFileSupport.isSupportedFile(file)
    }
}

/**
 * Project-level service wrapper for the Knip LSP TypeScript service.
 * Manages the lifecycle of the service instance.
 */
@Service(Service.Level.PROJECT)
internal class KnipLspServiceWrapper(project: Project) : Disposable {
    val service = KnipLspTypeScriptService(project)

    override fun dispose() {
        Disposer.dispose(service)
    }
}
