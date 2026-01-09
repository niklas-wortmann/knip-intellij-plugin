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
        group(KnipBundle.message("settingsGroupGeneral")) {
            row {
                checkBox(KnipBundle.message("settingsEnabled"))
                    .bindSelected(settings::enabled)
            }
        }

        group(KnipBundle.message("settingsGroupNodeConfig")) {
            row(KnipBundle.message("settingsNodePath")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileDescriptor()
                        .withTitle(KnipBundle.message("settingsSelectNodeExecutable")),
                    project
                ).bindText(settings::nodePath)
                    .comment(KnipBundle.message("settingsNodePathComment"))
            }
            row(KnipBundle.message("settingsLanguageServerPath")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileDescriptor()
                        .withTitle(KnipBundle.message("settingsSelectLanguageServer")),
                    project
                ).bindText(settings::languageServerPath)
                    .comment(KnipBundle.message("settingsLanguageServerPathComment"))
            }
        }

        group(KnipBundle.message("settingsGroupLanguageServer")) {
            row(KnipBundle.message("settingsServerArguments")) {
                textField()
                    .bindText(settings::serverArguments)
                    .comment(KnipBundle.message("settingsServerArgumentsComment"))
            }
        }
    }
}
