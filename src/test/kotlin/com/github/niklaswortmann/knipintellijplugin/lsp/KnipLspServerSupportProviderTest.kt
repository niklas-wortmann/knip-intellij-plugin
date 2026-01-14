package com.github.niklaswortmann.knipintellijplugin.lsp

import com.intellij.mock.MockVirtualFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for KnipLspServerSupportProvider utility methods.
 */
class KnipLspServerSupportProviderTest {

    // ==================== isSupportedFile tests ====================

    @Test
    fun `isSupportedFile returns true for JavaScript files`() {
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("app.js")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("component.jsx")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("module.mjs")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("config.cjs")))
    }

    @Test
    fun `isSupportedFile returns true for TypeScript files`() {
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("app.ts")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("component.tsx")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("module.mts")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("config.cts")))
    }

    @Test
    fun `isSupportedFile returns true for package json`() {
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("package.json")))
    }

    @Test
    fun `isSupportedFile returns true for knip config files`() {
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("knip.json")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("knip.jsonc")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("knip.ts")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("knip.config.ts")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("knip.config.js")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("knip.config.mjs")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("knip.config.cjs")))
    }

    @Test
    fun `isSupportedFile returns false for unsupported files`() {
        assertFalse(KnipLspServerSupportProvider.isSupportedFile(createMockFile("readme.md")))
        assertFalse(KnipLspServerSupportProvider.isSupportedFile(createMockFile("style.css")))
        assertFalse(KnipLspServerSupportProvider.isSupportedFile(createMockFile("image.png")))
        assertFalse(KnipLspServerSupportProvider.isSupportedFile(createMockFile("data.xml")))
        assertFalse(KnipLspServerSupportProvider.isSupportedFile(createMockFile("config.yaml")))
        assertFalse(KnipLspServerSupportProvider.isSupportedFile(createMockFile("Main.java")))
        assertFalse(KnipLspServerSupportProvider.isSupportedFile(createMockFile("Main.kt")))
    }

    @Test
    fun `isSupportedFile is case insensitive for extensions`() {
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("app.JS")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("app.TS")))
        assertTrue(KnipLspServerSupportProvider.isSupportedFile(createMockFile("app.Tsx")))
    }

    // ==================== isVersionAtLeast tests ====================

    @Test
    fun `isVersionAtLeast returns true for equal versions`() {
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.0.0", "1.0.0"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.1.0", "1.1.0"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("2.5.3", "2.5.3"))
    }

    @Test
    fun `isVersionAtLeast returns true for greater major version`() {
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("2.0.0", "1.0.0"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("3.0.0", "1.5.0"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("10.0.0", "9.9.9"))
    }

    @Test
    fun `isVersionAtLeast returns true for greater minor version`() {
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.2.0", "1.1.0"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.10.0", "1.9.0"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("2.1.0", "2.0.5"))
    }

    @Test
    fun `isVersionAtLeast returns true for greater patch version`() {
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.0.1", "1.0.0"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.1.5", "1.1.4"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("2.3.10", "2.3.9"))
    }

    @Test
    fun `isVersionAtLeast returns false for lesser major version`() {
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("1.0.0", "2.0.0"))
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("1.9.9", "2.0.0"))
    }

    @Test
    fun `isVersionAtLeast returns false for lesser minor version`() {
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("1.0.0", "1.1.0"))
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("2.4.9", "2.5.0"))
    }

    @Test
    fun `isVersionAtLeast returns false for lesser patch version`() {
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("1.0.0", "1.0.1"))
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("1.1.4", "1.1.5"))
    }

    @Test
    fun `isVersionAtLeast handles versions with different segment counts`() {
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.1", "1.0.0"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.1.0", "1.1"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("2", "1.9.9"))
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("1.0", "1.0.1"))
    }

    @Test
    fun `isVersionAtLeast handles real world knip versions`() {
        // Version 1.1.0 should be at least 1.1.0
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.1.0", "1.1.0"))

        // Version 1.2.0 should be at least 1.1.0
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.2.0", "1.1.0"))

        // Version 1.0.5 should NOT be at least 1.1.0
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("1.0.5", "1.1.0"))

        // Version 0.9.0 should NOT be at least 1.1.0
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("0.9.0", "1.1.0"))
    }

    @Test
    fun `isVersionAtLeast returns false for invalid version strings`() {
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("invalid", "1.0.0"))
        assertFalse(KnipLspServerSupportProvider.isVersionAtLeast("", "1.0.0"))
    }

    @Test
    fun `isVersionAtLeast handles versions with non-numeric parts`() {
        // Non-numeric parts are treated as 0
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("1.1.0-beta", "1.0.0"))
        assertTrue(KnipLspServerSupportProvider.isVersionAtLeast("2.0.0-alpha", "1.9.9"))
    }

    // ==================== Helper methods ====================

    private fun createMockFile(name: String): MockVirtualFile {
        return MockVirtualFile(name)
    }
}
