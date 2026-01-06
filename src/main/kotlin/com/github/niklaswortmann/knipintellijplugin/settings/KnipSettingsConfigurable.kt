package com.github.niklaswortmann.knipintellijplugin.settings

import com.github.niklaswortmann.knipintellijplugin.MyBundle
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
class KnipSettingsConfigurable(private val project: Project) : BoundConfigurable(MyBundle.message("settingsTitle")) {

    private val settings = KnipSettings.getInstance(project)

    override fun createPanel(): DialogPanel = panel {
        group("General") {
            row {
                checkBox(MyBundle.message("settingsEnabled"))
                    .bindSelected(settings::enabled)
            }
        }

        group("Node.js Configuration") {
            row(MyBundle.message("settingsNodePath")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileDescriptor()
                        .withTitle("Select Node.js Executable"),
                    project
                ).bindText(settings::nodePath)
                    .comment("Leave empty to use system PATH")
            }
            row("npx path:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileDescriptor()
                        .withTitle("Select npx Executable"),
                    project
                ).bindText(settings::npxPath)
                    .comment("Leave empty to auto-detect")
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
