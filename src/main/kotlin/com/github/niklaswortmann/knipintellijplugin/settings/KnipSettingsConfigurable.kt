package com.github.niklaswortmann.knipintellijplugin.settings

import com.github.niklaswortmann.knipintellijplugin.KnipBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

/**
 * Settings configurable for the Knip plugin.
 * Provides UI for configuring Node.js path, enable/disable, etc.
 */
class KnipSettingsConfigurable(private val project: Project) : BoundConfigurable(KnipBundle.message("settingsTitle")) {

    private val settings = KnipSettings.getInstance(project)

    override fun createPanel(): DialogPanel = panel {
        group("General") {
            row {
                checkBox(KnipBundle.message("settingsEnabled"))
                    .bindSelected(settings::enabled)
            }
        }

        group("Node.js Configuration") {
            row(KnipBundle.message("settingsNodePath")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileDescriptor()
                        .withTitle("Select Node.js Executable"),
                    project
                ).bindText(settings::nodePath)
                    .comment("Leave empty to use system PATH")
            }
            row("Language server path:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileDescriptor()
                        .withTitle("Select @knip/language-server Entry Point (src/index.js)"),
                    project
                ).bindText(settings::languageServerPath)
                    .comment("Leave empty to auto-detect. Install with: npm install -g @knip/language-server")
            }
        }

        group("Language Server") {
            row("Server arguments:") {
                textField()
                    .bindText(settings::serverArguments)
                    .comment("Arguments passed to the language server (default: --stdio)")
            }
        }
    }
}
