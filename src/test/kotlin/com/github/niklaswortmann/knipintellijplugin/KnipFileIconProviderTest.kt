package com.github.niklaswortmann.knipintellijplugin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for KnipFileIconProvider config file detection.
 */
class KnipFileIconProviderTest {

    @Test
    fun `isKnipConfigFile returns true for knip json config`() {
        assertTrue(KnipFileIconProvider.isKnipConfigFile("knip.json"))
        assertTrue(KnipFileIconProvider.isKnipConfigFile("knip.jsonc"))
    }

    @Test
    fun `isKnipConfigFile returns true for knip typescript config`() {
        assertTrue(KnipFileIconProvider.isKnipConfigFile("knip.ts"))
        assertTrue(KnipFileIconProvider.isKnipConfigFile("knip.config.ts"))
    }

    @Test
    fun `isKnipConfigFile returns true for knip javascript config`() {
        assertTrue(KnipFileIconProvider.isKnipConfigFile("knip.js"))
        assertTrue(KnipFileIconProvider.isKnipConfigFile("knip.config.js"))
        assertTrue(KnipFileIconProvider.isKnipConfigFile("knip.config.mjs"))
        assertTrue(KnipFileIconProvider.isKnipConfigFile("knip.config.cjs"))
    }

    @Test
    fun `isKnipConfigFile returns false for non-knip files`() {
        assertFalse(KnipFileIconProvider.isKnipConfigFile("package.json"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("tsconfig.json"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("eslint.config.js"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("vite.config.ts"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("index.ts"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("app.js"))
    }

    @Test
    fun `isKnipConfigFile returns false for similar but incorrect names`() {
        assertFalse(KnipFileIconProvider.isKnipConfigFile("knip.yaml"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("knip.config.yaml"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("myknip.json"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("knip-config.json"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("knip_config.ts"))
    }

    @Test
    fun `isKnipConfigFile is case sensitive`() {
        // Config file names should be exact matches (case sensitive)
        assertFalse(KnipFileIconProvider.isKnipConfigFile("Knip.json"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("KNIP.JSON"))
        assertFalse(KnipFileIconProvider.isKnipConfigFile("Knip.config.ts"))
    }

    @Test
    fun `isKnipConfigFile returns false for empty string`() {
        assertFalse(KnipFileIconProvider.isKnipConfigFile(""))
    }
}
