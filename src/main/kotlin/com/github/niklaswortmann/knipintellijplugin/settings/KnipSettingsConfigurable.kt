package com.github.niklaswortmann.knipintellijplugin.settings

import com.github.niklaswortmann.knipintellijplugin.KnipBundle
import com.github.niklaswortmann.knipintellijplugin.lsp.KnipLspServerLoader
import com.github.niklaswortmann.knipintellijplugin.lsp.KnipLspServerSupportProvider
import com.intellij.lang.typescript.lsp.bind
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel

/**
 * Settings configurable for the Knip plugin.
 * Provides UI for configuring the language server package and service mode.
 * Uses the same pattern as Astro plugin for NodePackageField integration.
 */
class KnipSettingsConfigurable(private val project: Project) : Configurable {

    private val settings = getKnipSettings(project)
    private lateinit var panel: DialogPanel

    override fun getDisplayName(): String = KnipBundle.message("settingsTitle")

    override fun createComponent(): DialogPanel {
        panel = panel {
            group(KnipBundle.message("settingsGroupGeneral")) {
                row(KnipBundle.message("settingsLanguageServerPackage")) {
                    cell(KnipLspServerLoader.createNodePackageField(project))
                        .align(AlignX.FILL)
                        .bind(settings::lspServerPackageRef)
                }

                buttonsGroup {
                    row {
                        radioButton(KnipBundle.message("settingsServiceDisabled"), KnipServiceMode.DISABLED)
                            .comment(KnipBundle.message("settingsServiceDisabledHelp"))
                    }
                    row {
                        radioButton(KnipBundle.message("settingsServiceEnabled"), KnipServiceMode.ENABLED)
                            .comment(KnipBundle.message("settingsServiceEnabledHelp"))
                    }
                }.bind(settings::serviceMode)
            }
        }
        return panel
    }

    override fun reset() {
        panel.reset()
    }

    override fun isModified(): Boolean = panel.isModified()

    override fun apply() {
        panel.apply()
        if (!project.isDefault) {
            LspServerManager.getInstance(project).stopAndRestartIfNeeded(KnipLspServerSupportProvider::class.java)
        }
    }
}
