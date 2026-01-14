package com.github.niklaswortmann.knipintellijplugin

import com.intellij.ide.IconProvider
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

/**
 * Provides the Knip icon for Knip configuration files in the project view and editor tabs.
 */
class KnipFileIconProvider : IconProvider() {

    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element !is PsiFile) return null

        val fileName = element.name
        if (isKnipConfigFile(fileName)) {
            return KNIP_ICON
        }

        return null
    }

    companion object {
        private val KNIP_ICON = IconLoader.getIcon("/icons/knip_16.png", KnipFileIconProvider::class.java)

        /**
         * Knip configuration file patterns.
         */
        private val KNIP_CONFIG_PATTERNS = setOf(
            "knip.json",
            "knip.jsonc",
            "knip.ts",
            "knip.js",
            "knip.config.ts",
            "knip.config.js",
            "knip.config.mjs",
            "knip.config.cjs"
        )

        internal fun isKnipConfigFile(fileName: String): Boolean {
            return fileName in KNIP_CONFIG_PATTERNS
        }
    }
}
