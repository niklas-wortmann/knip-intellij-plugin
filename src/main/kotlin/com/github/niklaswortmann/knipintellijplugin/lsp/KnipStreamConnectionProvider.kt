package com.github.niklaswortmann.knipintellijplugin.lsp

import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettings
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import java.io.File

/**
 * Connection provider that starts the Knip language server process.
 * Runs the @knip/language-server package directly with Node.js.
 * Supports various Node.js installation methods (system, nvm, volta, fnm, etc.)
 * 
 * Note: The @knip/language-server package does not have a bin field, so we cannot use npx.
 * Instead, we locate the installed package and run its entry point directly with node.
 */
class KnipStreamConnectionProvider(project: Project) : ProcessStreamConnectionProvider() {

    companion object {
        private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        private val userHome = System.getProperty("user.home")
        private const val PACKAGE_NAME = "@knip/language-server"
        private const val ENTRY_POINT = "src/index.js"
        
        /**
         * Common paths where Node.js might be installed
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
         * Find Node.js executable by searching common paths
         */
        fun findNodePath(): String {
            val nodeName = if (isWindows) "node.exe" else "node"
            
            // First, check if node is in PATH
            val pathEnv = System.getenv("PATH") ?: ""
            val pathSeparator = if (isWindows) ";" else ":"
            
            for (dir in pathEnv.split(pathSeparator)) {
                val nodeFile = File(dir, nodeName)
                if (nodeFile.exists() && nodeFile.canExecute()) {
                    return nodeFile.absolutePath
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
                        val nodeFile = File(versionDir, "bin/$nodeName")
                        if (nodeFile.exists() && nodeFile.canExecute()) {
                            return nodeFile.absolutePath
                        }
                    }
                } else {
                    val nodeFile = File(basePath, nodeName)
                    if (nodeFile.exists() && nodeFile.canExecute()) {
                        return nodeFile.absolutePath
                    }
                }
            }
            
            // Fallback to just "node" and hope it's in PATH
            return nodeName
        }
        
        /**
         * Find the @knip/language-server package installation path.
         * Searches in order:
         * 1. Project's local node_modules
         * 2. Volta global packages
         * 3. npm global packages
         * 4. nvm global packages
         * 5. pnpm global packages
         * 6. yarn global packages
         */
        fun findLanguageServerPath(projectBasePath: String?): String? {
            val packagePath = PACKAGE_NAME.replace("/", File.separator)
            
            // 1. Check project's local node_modules
            if (projectBasePath != null) {
                val localPath = File(projectBasePath, "node_modules/$packagePath/$ENTRY_POINT")
                if (localPath.exists()) {
                    return localPath.absolutePath
                }
            }
            
            // 2. Check Volta global packages
            val voltaPath = File(userHome, ".volta/tools/image/packages/$packagePath/lib/node_modules/$packagePath/$ENTRY_POINT")
            if (voltaPath.exists()) {
                return voltaPath.absolutePath
            }
            
            // 3. Check npm global packages (standard location)
            val npmGlobalPaths = if (isWindows) {
                listOf(
                    File(userHome, "AppData/Roaming/npm/node_modules/$packagePath/$ENTRY_POINT"),
                    File("C:/Program Files/nodejs/node_modules/$packagePath/$ENTRY_POINT")
                )
            } else {
                listOf(
                    File("/usr/local/lib/node_modules/$packagePath/$ENTRY_POINT"),
                    File("/usr/lib/node_modules/$packagePath/$ENTRY_POINT"),
                    File(userHome, ".npm-global/lib/node_modules/$packagePath/$ENTRY_POINT")
                )
            }
            for (path in npmGlobalPaths) {
                if (path.exists()) {
                    return path.absolutePath
                }
            }
            
            // 4. Check nvm global packages (search all installed versions)
            val nvmBase = File(userHome, ".nvm/versions/node")
            if (nvmBase.exists()) {
                val versionDirs = nvmBase.listFiles()?.filter { it.isDirectory }?.sortedDescending()
                versionDirs?.forEach { versionDir ->
                    val nvmPath = File(versionDir, "lib/node_modules/$packagePath/$ENTRY_POINT")
                    if (nvmPath.exists()) {
                        return nvmPath.absolutePath
                    }
                }
            }
            
            // 5. Check pnpm global packages
            val pnpmPath = File(userHome, ".local/share/pnpm/global/5/node_modules/$packagePath/$ENTRY_POINT")
            if (pnpmPath.exists()) {
                return pnpmPath.absolutePath
            }
            
            // 6. Check yarn global packages
            val yarnPaths = listOf(
                File(userHome, ".yarn/global/node_modules/$packagePath/$ENTRY_POINT"),
                File(userHome, ".config/yarn/global/node_modules/$packagePath/$ENTRY_POINT")
            )
            for (path in yarnPaths) {
                if (path.exists()) {
                    return path.absolutePath
                }
            }
            
            // 7. Check fnm global packages
            val fnmBase = File(userHome, ".fnm/node-versions")
            if (fnmBase.exists()) {
                val versionDirs = fnmBase.listFiles()?.filter { it.isDirectory }?.sortedDescending()
                versionDirs?.forEach { versionDir ->
                    val fnmPath = File(versionDir, "installation/lib/node_modules/$packagePath/$ENTRY_POINT")
                    if (fnmPath.exists()) {
                        return fnmPath.absolutePath
                    }
                }
            }
            
            // 8. Check asdf global packages
            val asdfBase = File(userHome, ".asdf/installs/nodejs")
            if (asdfBase.exists()) {
                val versionDirs = asdfBase.listFiles()?.filter { it.isDirectory }?.sortedDescending()
                versionDirs?.forEach { versionDir ->
                    val asdfPath = File(versionDir, "lib/node_modules/$packagePath/$ENTRY_POINT")
                    if (asdfPath.exists()) {
                        return asdfPath.absolutePath
                    }
                }
            }
            
            return null
        }
    }

    init {
        val settings = KnipSettings.getInstance(project)
        val commands = mutableListOf<String>()
        
        // Use custom node path from settings, or auto-detect
        val nodePath = if (settings.nodePath.isNotBlank()) {
            settings.nodePath
        } else {
            findNodePath()
        }
        
        // Use custom language server path from settings, or auto-detect
        val languageServerPath = if (settings.languageServerPath.isNotBlank()) {
            settings.languageServerPath
        } else {
            findLanguageServerPath(project.basePath)
        }
        
        if (languageServerPath != null) {
            commands.add(nodePath)
            commands.add(languageServerPath)
            
            // Add server arguments from settings
            settings.serverArguments.split(" ")
                .filter { it.isNotBlank() }
                .forEach { commands.add(it) }
        } else {
            // Fallback: show error message - the server won't start but we need valid commands
            // The error will be caught by LSP4IJ and shown to the user
            commands.add(nodePath)
            commands.add("-e")
            commands.add("console.error('Error: @knip/language-server package not found. Please install it globally with: npm install -g @knip/language-server'); process.exit(1);")
        }
        
        super.setCommands(commands)
        
        // Set the working directory to the project base path
        project.basePath?.let { basePath ->
            super.setWorkingDirectory(basePath)
        }
    }
}
