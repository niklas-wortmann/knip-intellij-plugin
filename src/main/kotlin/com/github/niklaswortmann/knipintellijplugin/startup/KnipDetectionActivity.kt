package com.github.niklaswortmann.knipintellijplugin.startup

import com.github.niklaswortmann.knipintellijplugin.settings.KnipApplicationSettings
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile

/**
 * Project startup activity that detects if Knip is used in the project.
 * 
 * Detection is based on:
 * 1. Presence of "knip" in package.json dependencies or devDependencies
 * 2. Presence of knip configuration files (knip.json, knip.jsonc, knip.ts)
 * 
 * When Knip is detected, a notification is shown suggesting to use the plugin.
 */
class KnipDetectionActivity : ProjectActivity {

    companion object {
        private val LOG = Logger.getInstance(KnipDetectionActivity::class.java)
        private const val NOTIFICATION_GROUP_ID = "Knip"
        private const val MARKETPLACE_URL = "https://plugins.jetbrains.com/plugin/MARKETPLACE_ID"
        
        // Knip package names to detect
        private val KNIP_PACKAGES = setOf("knip")
        
        // Knip config file names
        private val KNIP_CONFIG_FILES = setOf("knip.json", "knip.jsonc", "knip.ts", "knip.config.ts", "knip.config.js")
    }

    override suspend fun execute(project: Project) {
        val appSettings = KnipApplicationSettings.getInstance()
        
        // Don't show if user has dismissed the suggestion
        if (appSettings.dismissedPluginSuggestion) {
            LOG.debug("Plugin suggestion was previously dismissed, skipping detection")
            return
        }

        // Check if Knip is used in this project
        if (isKnipUsedInProject(project)) {
            LOG.info("Knip detected in project ${project.name}")
            showPluginSuggestionNotification(project)
        }
    }

    /**
     * Checks if Knip is used in the project by examining package.json and config files.
     */
    private fun isKnipUsedInProject(project: Project): Boolean {
        val projectDir = project.guessProjectDir() ?: return false
        
        // Check for knip config files first (quick check)
        if (hasKnipConfigFile(projectDir)) {
            LOG.debug("Found Knip config file in project")
            return true
        }
        
        // Check package.json for knip dependency
        if (hasKnipInPackageJson(projectDir)) {
            LOG.debug("Found Knip in package.json")
            return true
        }
        
        return false
    }

    /**
     * Checks if any Knip configuration file exists in the project root.
     */
    private fun hasKnipConfigFile(projectDir: VirtualFile): Boolean {
        return KNIP_CONFIG_FILES.any { configFile ->
            projectDir.findChild(configFile) != null
        }
    }

    /**
     * Checks if Knip is listed in package.json dependencies or devDependencies.
     */
    private fun hasKnipInPackageJson(projectDir: VirtualFile): Boolean {
        val packageJson = projectDir.findChild("package.json") ?: return false
        
        return try {
            val content = String(packageJson.contentsToByteArray(), Charsets.UTF_8)
            val jsonObject = JsonParser.parseString(content).asJsonObject
            
            // Check dependencies
            val dependencies = jsonObject.getAsJsonObject("dependencies")
            if (dependencies != null && hasKnipPackage(dependencies)) {
                return true
            }
            
            // Check devDependencies
            val devDependencies = jsonObject.getAsJsonObject("devDependencies")
            if (devDependencies != null && hasKnipPackage(devDependencies)) {
                return true
            }
            
            false
        } catch (e: Exception) {
            LOG.debug("Failed to parse package.json: ${e.message}")
            false
        }
    }

    /**
     * Checks if any Knip package is present in the given dependencies object.
     */
    private fun hasKnipPackage(dependencies: JsonObject): Boolean {
        return KNIP_PACKAGES.any { packageName ->
            dependencies.has(packageName)
        }
    }

    /**
     * Shows a notification suggesting to use the Knip plugin.
     */
    private fun showPluginSuggestionNotification(project: Project) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(
                "Knip Detected",
                "This project uses Knip for finding unused code. The Knip plugin is active and ready to help!",
                NotificationType.INFORMATION
            )
            .addAction(NotificationAction.createSimple("Open Knip Settings") {
                com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                    .showSettingsDialog(project, "Knip")
            })
        
        notification.addAction(NotificationAction.createSimple("Don't Show Again") {
            KnipApplicationSettings.getInstance().dismissedPluginSuggestion = true
            notification.expire()
        })

        notification.notify(project)
    }
}
