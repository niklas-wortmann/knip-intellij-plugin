<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Knip IntelliJ Plugin Changelog

## [Unreleased]

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
