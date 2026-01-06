# Knip IntelliJ Plugin

![Build](https://github.com/niklas-wortmann/knip-intellij-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
**Knip** - Find unused files, dependencies and exports in your JavaScript/TypeScript projects.

This plugin integrates the [Knip language server](https://knip.dev) into JetBrains IDEs, providing real-time detection of:
- Unused files
- Unused dependencies
- Unused exports
- Unused class members
- Unused types

Knip helps you keep your codebase clean and maintainable by identifying dead code that can be safely removed.

**Features:**
- Real-time diagnostics in the editor
- Support for JavaScript, TypeScript, JSX, TSX, and JSON files
- Automatic language server lifecycle management
- Configuration via knip.json or knip.ts
<!-- Plugin description end -->

## Requirements

- **Node.js** (v18 or later recommended)
- **npm/npx** available in your PATH
- A JetBrains IDE (IntelliJ IDEA, WebStorm, PhpStorm, etc.)

## Installation

### From JetBrains Marketplace

1. Open your IDE
2. Go to <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd>
3. Search for "Knip"
4. Click <kbd>Install</kbd>
5. Restart your IDE

### Manual Installation

1. Download the [latest release](https://github.com/niklas-wortmann/knip-intellij-plugin/releases/latest)
2. Go to <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
3. Select the downloaded ZIP file
4. Restart your IDE

## Configuration

### Plugin Settings

Configure the plugin at <kbd>Settings/Preferences</kbd> > <kbd>Tools</kbd> > <kbd>Knip</kbd>:

| Setting | Description |
|---------|-------------|
| **Enable Knip** | Toggle the language server on/off |
| **Node.js path** | Custom path to Node.js executable (leave empty to use system PATH) |
| **npx path** | Custom path to npx executable (leave empty to auto-detect) |
| **Server arguments** | Arguments passed to the language server (default: `--stdio`) |

### Project Configuration

Knip uses configuration files in your project root. Create one of the following:

- `knip.json` - JSON configuration
- `knip.jsonc` - JSON with comments
- `knip.ts` - TypeScript configuration

Example `knip.json`:
```json
{
  "$schema": "https://unpkg.com/knip@latest/schema.json",
  "entry": ["src/index.ts"],
  "project": ["src/**/*.ts"]
}
```

For more configuration options, see the [Knip documentation](https://knip.dev/overview/configuration).

## Usage

Once installed and configured:

1. Open a JavaScript/TypeScript project
2. The Knip language server starts automatically
3. Unused code is highlighted in the editor with warnings
4. View all issues in the **Problems** tool window

### Restart Language Server

If you need to restart the language server:
- Go to <kbd>Tools</kbd> > <kbd>Restart Knip Language Server</kbd>
- Or use <kbd>Find Action</kbd> (Ctrl+Shift+A / Cmd+Shift+A) and search for "Restart Knip"

## Supported File Types

- JavaScript (`.js`)
- TypeScript (`.ts`)
- JSX (`.jsx`)
- TSX (`.tsx`)
- JSON (`.json`) - for `package.json` and `knip.json`

## Troubleshooting

### Language server not starting

1. Ensure Node.js is installed and available in your PATH
2. Try running `npx @knip/language-server --version` in your terminal
3. Check the IDE log for errors: <kbd>Help</kbd> > <kbd>Show Log in Finder/Explorer</kbd>
4. Configure a custom Node.js/npx path in settings if auto-detection fails

### No diagnostics appearing

1. Ensure your project has a valid Knip configuration file
2. Check that the file types are supported
3. Try restarting the language server

## Development

### Building from Source

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Running the Plugin

```bash
./gradlew runIde
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Links

- [Knip](https://knip.dev) - The underlying tool for finding unused code
- [@knip/language-server](https://www.npmjs.com/package/@knip/language-server) - The language server this plugin uses
- [LSP4IJ](https://github.com/redhat-developer/lsp4ij) - LSP support for IntelliJ

---
Plugin based on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
