package com.github.niklaswortmann.knipintellijplugin.filetype

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * File type for Knip configuration files.
 * 
 * This file type is registered to enable automatic plugin discovery.
 * When a user opens a Knip config file (knip.json, knip.jsonc, etc.) in an IDE
 * without this plugin installed, the IDE will suggest installing the Knip plugin.
 * 
 * @see <a href="https://plugins.jetbrains.com/docs/marketplace/intellij-plugin-recommendations.html#file-type">Plugin Recommendations - File Type</a>
 */
class KnipConfigFileType private constructor() : FileType {

    override fun getName(): String = NAME

    override fun getDescription(): String = "Knip configuration file"

    override fun getDefaultExtension(): String = ""

    override fun getIcon(): Icon = ICON

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = false

    companion object {
        const val NAME = "Knip Config"
        
        @JvmStatic
        val ICON: Icon = IconLoader.getIcon("/icons/knip_16.png", KnipConfigFileType::class.java)
        
        @JvmStatic
        val INSTANCE = KnipConfigFileType()
    }
}
