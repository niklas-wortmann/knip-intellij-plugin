<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Knip IntelliJ Plugin Changelog

## [Unreleased]

### Changed
- Migrated from LSP4IJ (RedHat) to JetBrains Platform LSP API for native LSP support
- Plugin now requires IntelliJ IDEA Ultimate or other commercial JetBrains IDEs with LSP support

### Technical
- Replaced `com.redhat.devtools.lsp4ij` dependency with `com.intellij.modules.ultimate`
- New implementation using `LspServerSupportProvider` and `ProjectWideLspServerDescriptor`
- Custom `knip.start` request handling via coroutines and `LspServer.sendRequest`

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
  - Custom npx path
  - Server arguments configuration
- "Restart Knip Language Server" action in Tools menu
- Automatic language server lifecycle management
- User notifications for server status and errors

### Technical
- Built on IntelliJ Platform Plugin Template
- Uses LSP4IJ for Language Server Protocol support
- Supports IntelliJ IDEA 2025.2+ and compatible JetBrains IDEs
