package com.github.niklaswortmann.knipintellijplugin.lsp

import com.github.niklaswortmann.knipintellijplugin.settings.KnipSettings
import com.intellij.openapi.diagnostic.Logger
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
        private val LOG = Logger.getInstance(KnipStreamConnectionProvider::class.java)
        private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        private val userHome = System.getProperty("user.home")
        private const val PACKAGE_NAME = "@knip/language-server"
        private const val ENTRY_POINT = "src/index.js"

        // Cache for node path to avoid repeated filesystem searches
        @Volatile
        private var cachedNodePath: String? = null

        // Cache for language server paths per project to avoid repeated searches
        private val languageServerPathCache = mutableMapOf<String?, String?>()
        
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
         * Find Node.js executable by searching common paths.
         * Results are cached to improve performance on subsequent calls.
         */
        fun findNodePath(): String {
            // Return cached value if available
            cachedNodePath?.let {
                LOG.debug("Using cached node path: $it")
                return it
            }

            LOG.info("Searching for Node.js executable...")
            val nodeName = if (isWindows) "node.exe" else "node"

            // First, check if node is in PATH
            val pathEnv = System.getenv("PATH") ?: ""
            val pathSeparator = if (isWindows) ";" else ":"

            for (dir in pathEnv.split(pathSeparator)) {
                try {
                    val nodeFile = File(dir, nodeName)
                    if (nodeFile.exists() && nodeFile.canExecute()) {
                        val path = nodeFile.absolutePath
                        cachedNodePath = path // Cache the result
                        LOG.info("Found Node.js in PATH: $path")
                        return path
                    }
                } catch (e: SecurityException) {
                    LOG.debug("Security exception while checking directory: $dir", e)
                }
            }
            
            // Check common installation paths
            LOG.debug("Checking common Node.js installation paths...")
            for (basePath in getCommonNodePaths()) {
                try {
                    // Handle nvm which has version subdirectories
                    val baseDir = File(basePath)
                    if (basePath.contains(".nvm/versions/node") && baseDir.exists()) {
                        // Find the latest version directory
                        val versionDirs = baseDir.listFiles()?.filter { it.isDirectory }?.sortedDescending()
                        versionDirs?.firstOrNull()?.let { versionDir ->
                            val nodeFile = File(versionDir, "bin/$nodeName")
                            if (nodeFile.exists() && nodeFile.canExecute()) {
                                val path = nodeFile.absolutePath
                                cachedNodePath = path // Cache the result
                                LOG.info("Found Node.js via nvm: $path")
                                return path
                            }
                        }
                    } else {
                        val nodeFile = File(basePath, nodeName)
                        if (nodeFile.exists() && nodeFile.canExecute()) {
                            val path = nodeFile.absolutePath
                            cachedNodePath = path // Cache the result
                            LOG.info("Found Node.js at: $path")
                            return path
                        }
                    }
                } catch (e: Exception) {
                    LOG.debug("Error checking path $basePath: ${e.message}")
                }
            }

            // Fallback to just "node" and hope it's in PATH
            LOG.warn("Node.js executable not found in common paths, falling back to 'node' command")
            cachedNodePath = nodeName // Cache the fallback
            return nodeName
        }
        
        /**
         * Find the @knip/language-server package installation path.
         * Results are cached per project to improve performance.
         * Searches in order:
         * 1. Project's local node_modules
         * 2. Volta global packages
         * 3. npm global packages
         * 4. nvm global packages
         * 5. pnpm global packages
         * 6. yarn global packages
         */
        fun findLanguageServerPath(projectBasePath: String?): String? {
            // Check cache first
            languageServerPathCache[projectBasePath]?.let {
                // Verify cached path still exists
                if (File(it).exists()) {
                    LOG.debug("Using cached language server path: $it")
                    return it
                } else {
                    // Remove stale cache entry
                    LOG.warn("Cached language server path no longer exists: $it")
                    languageServerPathCache.remove(projectBasePath)
                }
            }

            LOG.info("Searching for @knip/language-server package...")
            val packagePath = PACKAGE_NAME.replace("/", File.separator)
            
            // 1. Check project's local node_modules
            if (projectBasePath != null) {
                try {
                    val localPath = File(projectBasePath, "node_modules/$packagePath/$ENTRY_POINT")
                    if (localPath.exists()) {
                        val path = localPath.absolutePath
                        languageServerPathCache[projectBasePath] = path // Cache the result
                        LOG.info("Found language server in project node_modules: $path")
                        return path
                    }
                } catch (e: Exception) {
                    LOG.debug("Error checking project node_modules: ${e.message}")
                }
            }
            
            // 2. Check Volta global packages
            val voltaPath = File(userHome, ".volta/tools/image/packages/$packagePath/lib/node_modules/$packagePath/$ENTRY_POINT")
            if (voltaPath.exists()) {
                val path = voltaPath.absolutePath
                languageServerPathCache[projectBasePath] = path // Cache the result
                return path
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
                    val absolutePath = path.absolutePath
                    languageServerPathCache[projectBasePath] = absolutePath // Cache the result
                    return absolutePath
                }
            }
            
            // 4. Check nvm global packages (search all installed versions)
            val nvmBase = File(userHome, ".nvm/versions/node")
            if (nvmBase.exists()) {
                val versionDirs = nvmBase.listFiles()?.filter { it.isDirectory }?.sortedDescending()
                versionDirs?.forEach { versionDir ->
                    val nvmPath = File(versionDir, "lib/node_modules/$packagePath/$ENTRY_POINT")
                    if (nvmPath.exists()) {
                        val path = nvmPath.absolutePath
                        languageServerPathCache[projectBasePath] = path // Cache the result
                        return path
                    }
                }
            }
            
            // 5. Check pnpm global packages
            val pnpmPath = File(userHome, ".local/share/pnpm/global/5/node_modules/$packagePath/$ENTRY_POINT")
            if (pnpmPath.exists()) {
                val path = pnpmPath.absolutePath
                languageServerPathCache[projectBasePath] = path // Cache the result
                return path
            }
            
            // 6. Check yarn global packages
            val yarnPaths = listOf(
                File(userHome, ".yarn/global/node_modules/$packagePath/$ENTRY_POINT"),
                File(userHome, ".config/yarn/global/node_modules/$packagePath/$ENTRY_POINT")
            )
            for (path in yarnPaths) {
                if (path.exists()) {
                    val absolutePath = path.absolutePath
                    languageServerPathCache[projectBasePath] = absolutePath // Cache the result
                    return absolutePath
                }
            }
            
            // 7. Check fnm global packages
            val fnmBase = File(userHome, ".fnm/node-versions")
            if (fnmBase.exists()) {
                val versionDirs = fnmBase.listFiles()?.filter { it.isDirectory }?.sortedDescending()
                versionDirs?.forEach { versionDir ->
                    val fnmPath = File(versionDir, "installation/lib/node_modules/$packagePath/$ENTRY_POINT")
                    if (fnmPath.exists()) {
                        val path = fnmPath.absolutePath
                        languageServerPathCache[projectBasePath] = path // Cache the result
                        return path
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
                        val path = asdfPath.absolutePath
                        languageServerPathCache[projectBasePath] = path // Cache the result
                        return path
                    }
                }
            }

            // Cache null result to avoid repeated searches
            LOG.warn("@knip/language-server package not found in any standard location")
            languageServerPathCache[projectBasePath] = null
            return null
        }
    }

    init {
        val settings = KnipSettings.getInstance(project)
        val commands = mutableListOf<String>()

        try {
            // Use custom node path from settings, or auto-detect
            val nodePath = if (settings.nodePath.isNotBlank()) {
                LOG.info("Using custom node path from settings: ${settings.nodePath}")
                settings.nodePath
            } else {
                findNodePath()
            }

            // Use custom language server path from settings, or auto-detect
            val languageServerPath = if (settings.languageServerPath.isNotBlank()) {
                LOG.info("Using custom language server path from settings: ${settings.languageServerPath}")
                settings.languageServerPath
            } else {
                findLanguageServerPath(project.basePath)
            }

            if (languageServerPath != null) {
                commands.add(nodePath)
                commands.add(languageServerPath)

                // Add server arguments from settings
                val args = settings.serverArguments.split(" ")
                    .filter { it.isNotBlank() }
                args.forEach { commands.add(it) }

                LOG.info("Starting Knip language server with command: ${commands.joinToString(" ")}")
            } else {
                // Fallback: show error message - the server won't start but we need valid commands
                // The error will be caught by LSP4IJ and shown to the user
                LOG.error("Language server path not found, creating fallback error command")
                commands.add(nodePath)
                commands.add("-e")
                commands.add("console.error('Error: @knip/language-server package not found. Please install it globally with: npm install -g @knip/language-server'); process.exit(1);")
            }

            super.setCommands(commands)

            // Set the working directory to the project base path
            project.basePath?.let { basePath ->
                LOG.debug("Setting working directory to: $basePath")
                super.setWorkingDirectory(basePath)
            }
        } catch (e: Exception) {
            LOG.error("Error initializing Knip connection provider", e)
            throw e
        }
    }
}
