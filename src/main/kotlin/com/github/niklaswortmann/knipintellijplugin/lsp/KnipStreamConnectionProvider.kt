package com.github.niklaswortmann.knipintellijplugin.lsp

import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettings
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import java.io.File

/**
 * Connection provider that starts the Knip language server process.
 * Uses npx to run @knip/language-server with stdio communication.
 * Supports various Node.js installation methods (system, nvm, volta, fnm, etc.)
 */
class KnipStreamConnectionProvider(project: Project) : ProcessStreamConnectionProvider() {

    companion object {
        private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        private val userHome = System.getProperty("user.home")
        
        /**
         * Common paths where Node.js/npx might be installed
         */
        private fun getCommonNodePaths(): List<String> {
            return if (isWindows) {
                listOf(
                    // Standard Windows paths
                    "C:\\Program Files\\nodejs",
                    "C:\\Program Files (x86)\\nodejs",
                    // nvm-windows
                    "$userHome\\AppData\\Roaming\\nvm",
                    // volta
                    "$userHome\\.volta\\bin",
                    // fnm
                    "$userHome\\.fnm",
                    // Scoop
                    "$userHome\\scoop\\apps\\nodejs\\current"
                )
            } else {
                listOf(
                    // Standard Unix paths
                    "/usr/local/bin",
                    "/usr/bin",
                    "/opt/homebrew/bin",
                    // nvm
                    "$userHome/.nvm/versions/node",
                    // volta
                    "$userHome/.volta/bin",
                    // fnm
                    "$userHome/.fnm/current/bin",
                    // asdf
                    "$userHome/.asdf/shims",
                    // Homebrew on Intel Mac
                    "/usr/local/opt/node/bin",
                    // Homebrew on Apple Silicon
                    "/opt/homebrew/opt/node/bin"
                )
            }
        }
        
        /**
         * Find npx executable by searching common paths
         */
        fun findNpxPath(): String {
            val npxName = if (isWindows) "npx.cmd" else "npx"
            
            // First, check if npx is in PATH
            val pathEnv = System.getenv("PATH") ?: ""
            val pathSeparator = if (isWindows) ";" else ":"
            
            for (dir in pathEnv.split(pathSeparator)) {
                val npxFile = File(dir, npxName)
                if (npxFile.exists() && npxFile.canExecute()) {
                    return npxFile.absolutePath
                }
            }
            
            // Check common installation paths
            for (basePath in getCommonNodePaths()) {
                // Handle nvm which has version subdirectories
                val baseDir = File(basePath)
                if (basePath.contains(".nvm/versions/node") && baseDir.exists()) {
                    // Find the latest version directory
                    val versionDirs = baseDir.listFiles()?.filter { it.isDirectory }?.sortedDescending()
                    versionDirs?.firstOrNull()?.let { versionDir ->
                        val npxFile = File(versionDir, "bin/$npxName")
                        if (npxFile.exists() && npxFile.canExecute()) {
                            return npxFile.absolutePath
                        }
                    }
                } else {
                    val npxFile = File(basePath, npxName)
                    if (npxFile.exists() && npxFile.canExecute()) {
                        return npxFile.absolutePath
                    }
                }
            }
            
            // Fallback to just "npx" and hope it's in PATH
            return npxName
        }
    }

    init {
        val settings = KnipSettings.getInstance(project)
        val commands = mutableListOf<String>()
        
        // Use custom npx path from settings, or auto-detect
        val npxPath = if (settings.npxPath.isNotBlank()) {
            settings.npxPath
        } else {
            findNpxPath()
        }
        
        commands.add(npxPath)
        commands.add("@knip/language-server")
        
        // Add server arguments from settings
        settings.serverArguments.split(" ")
            .filter { it.isNotBlank() }
            .forEach { commands.add(it) }
        
        super.setCommands(commands)
        
        // Set the working directory to the project base path
        project.basePath?.let { basePath ->
            super.setWorkingDirectory(basePath)
        }
    }
}
