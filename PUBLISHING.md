# Publishing to JetBrains Marketplace

This guide explains how to publish the Knip plugin to the JetBrains Marketplace.

## Prerequisites

1. **JetBrains Account**: Create an account at [JetBrains Hub](https://hub.jetbrains.com/)
2. **Vendor Registration**: Register as a plugin vendor at [JetBrains Marketplace](https://plugins.jetbrains.com/author/me)

## One-Time Setup

### 1. Generate Plugin Signing Keys

Plugin signing is required for marketplace distribution. Generate a certificate chain:

```bash
# Generate private key
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:4096

# Generate certificate signing request
openssl req -new -key private.pem -out request.csr

# Generate self-signed certificate (valid for 1 year)
openssl x509 -req -days 365 -in request.csr -signkey private.pem -out chain.crt
```

### 2. Configure GitHub Secrets

Add the following secrets to your GitHub repository (Settings > Secrets and variables > Actions):

| Secret | Description |
|--------|-------------|
| `PRIVATE_KEY` | Contents of `private.pem` |
| `PRIVATE_KEY_PASSWORD` | Password for the private key (if set) |
| `CERTIFICATE_CHAIN` | Contents of `chain.crt` |
| `PUBLISH_TOKEN` | JetBrains Marketplace token (see below) |

### 3. Generate Marketplace Token

1. Go to [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. Click your profile > **Marketplace Settings**
3. Go to **My Tokens** tab
4. Click **Generate Token**
5. Copy the token and add it as `PUBLISH_TOKEN` secret

## Publishing Process

### Automatic Publishing (Recommended)

The GitHub Actions workflow automatically publishes when you create a release:

1. Update version in `gradle.properties`:
   ```properties
   pluginVersion = 1.0.0
   ```

2. Commit and push the version change

3. Create a GitHub release:
   - Go to **Releases** > **Create a new release**
   - Create a new tag (e.g., `v1.0.0`)
   - Add release notes
   - Publish the release

4. The workflow will automatically:
   - Build the plugin
   - Sign the plugin
   - Upload to JetBrains Marketplace

### Manual Publishing

To publish manually:

```bash
# Set environment variables
export PUBLISH_TOKEN="your-marketplace-token"
export PRIVATE_KEY="$(cat private.pem)"
export CERTIFICATE_CHAIN="$(cat chain.crt)"

# Build and publish
./gradlew publishPlugin
```

## First-Time Publication

For the first publication:

1. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```

2. Go to [JetBrains Marketplace](https://plugins.jetbrains.com/)

3. Click **Upload plugin**

4. Upload the ZIP file from `build/distributions/`

5. Fill in the plugin details:
   - **Name**: Knip
   - **Description**: (auto-extracted from README)
   - **Vendor**: Your vendor name
   - **Tags**: JavaScript, TypeScript, Linting, Code Quality
   - **License**: MIT

6. Submit for review

## Post-Publication

After the plugin is approved:

1. Update `MARKETPLACE_ID` in README badges with your plugin ID
2. The plugin ID can be found in the URL: `https://plugins.jetbrains.com/plugin/XXXXX-knip`

## Version Guidelines

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** (1.0.0): Breaking changes
- **MINOR** (0.1.0): New features, backward compatible
- **PATCH** (0.0.1): Bug fixes, backward compatible

## Changelog

Update `CHANGELOG.md` before each release with:
- New features
- Bug fixes
- Breaking changes
- Deprecations

## Troubleshooting

### Plugin rejected

Common reasons:
- Missing or invalid plugin description
- Compatibility issues with specified IDE versions
- Missing vendor information

### Signing errors

- Ensure private key and certificate are correctly formatted
- Check that the certificate hasn't expired
- Verify the certificate chain is complete

### Build failures

```bash
# Clean and rebuild
./gradlew clean build

# Check for dependency issues
./gradlew dependencies
```
