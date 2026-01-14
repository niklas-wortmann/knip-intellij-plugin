<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Knip IntelliJ Plugin Changelog

## [Unreleased]

### Added
- Knip icon displayed for configuration files in project view and editor tabs
- Progress indicator while Knip analyzes the project (requires language server v1.1.0+)
- LSP status bar widget with Knip icon showing server status
- Support for `knip.moduleGraphBuilt` notification from language server v1.1.0+
- Version detection and logging for the language server
- Additional file extension support: `.mjs`, `.cjs`, `.mts`, `.cts`, `.jsonc`

### Changed
- Migrated from LSP4IJ (RedHat) to JetBrains Platform LSP API for native LSP support
- Restart functionality moved from Tools menu to LSP status bar widget
- Plugin now requires IntelliJ IDEA Ultimate or other commercial JetBrains IDEs with LSP support

### Removed
- "Restart Knip Language Server" action from Tools menu (use status bar widget instead)

### Technical
- Replaced `com.redhat.devtools.lsp4ij` dependency with `com.intellij.modules.ultimate`
- New implementation using `LspServerSupportProvider` and `ProjectWideLspServerDescriptor`
- Custom `knip.start` request handling via coroutines and `LspServer.sendRequest`
- Custom `Lsp4jClient` implementation to handle `knip.moduleGraphBuilt` notification
- Added `KnipFileIconProvider` for config file icons
- Semantic version comparison for feature detection

## [0.0.1] - 2026-01-06
### Added
- Initial release of Knip IntelliJ Plugin
- Integration with [@knip/language-server](https://www.npmjs.com/package/@knip/language-server) via LSP4IJ
- Real-time diagnostics for unused code detection:
  - Unused files
  - Unused dependencies
  - Unused exports
  - Unused class members
  - Unused types
- Support for JavaScript, TypeScript, JSX, TSX, and JSON files
- Automatic Node.js/npx detection supporting:
  - System PATH
  - nvm (Node Version Manager)
  - volta
  - fnm
  - asdf
  - Homebrew installations
- Plugin settings page (Settings > Tools > Knip):
  - Enable/disable toggle
  - Custom Node.js path
  - Custom language server path
  - Server arguments configuration
- Automatic language server lifecycle management
- User notifications for server status and errors

### Technical
- Built on IntelliJ Platform Plugin Template
- Uses LSP4IJ for Language Server Protocol support
- Supports IntelliJ IDEA 2025.2+ and compatible JetBrains IDEs
