package com.github.niklaswortmann.knipintellijplugin.startup

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for KnipDetectionActivity JSON parsing logic.
 */
class KnipDetectionActivityTest {

    companion object {
        private val KNIP_PACKAGES = setOf("knip", "@knip/knip")
    }

    @Test
    fun testDetectsKnipInDevDependencies() {
        val packageJson = """
            {
                "name": "test-project",
                "devDependencies": {
                    "knip": "^5.0.0",
                    "typescript": "^5.0.0"
                }
            }
        """.trimIndent()

        val jsonObject = JsonParser.parseString(packageJson).asJsonObject
        val devDependencies = jsonObject.getAsJsonObject("devDependencies")
        
        assertNotNull(devDependencies)
        assertTrue(hasKnipPackage(devDependencies))
    }

    @Test
    fun testDetectsKnipInDependencies() {
        val packageJson = """
            {
                "name": "test-project",
                "dependencies": {
                    "knip": "^5.0.0"
                }
            }
        """.trimIndent()

        val jsonObject = JsonParser.parseString(packageJson).asJsonObject
        val dependencies = jsonObject.getAsJsonObject("dependencies")
        
        assertNotNull(dependencies)
        assertTrue(hasKnipPackage(dependencies))
    }

    @Test
    fun testDoesNotDetectKnipWhenAbsent() {
        val packageJson = """
            {
                "name": "test-project",
                "devDependencies": {
                    "typescript": "^5.0.0",
                    "eslint": "^8.0.0"
                }
            }
        """.trimIndent()

        val jsonObject = JsonParser.parseString(packageJson).asJsonObject
        val devDependencies = jsonObject.getAsJsonObject("devDependencies")
        
        assertNotNull(devDependencies)
        assertFalse(hasKnipPackage(devDependencies))
    }

    @Test
    fun testHandlesEmptyDependencies() {
        val packageJson = """
            {
                "name": "test-project",
                "devDependencies": {}
            }
        """.trimIndent()

        val jsonObject = JsonParser.parseString(packageJson).asJsonObject
        val devDependencies = jsonObject.getAsJsonObject("devDependencies")
        
        assertNotNull(devDependencies)
        assertFalse(hasKnipPackage(devDependencies))
    }

    @Test
    fun testHandlesMissingDependencies() {
        val packageJson = """
            {
                "name": "test-project"
            }
        """.trimIndent()

        val jsonObject = JsonParser.parseString(packageJson).asJsonObject
        val dependencies = jsonObject.getAsJsonObject("dependencies")
        val devDependencies = jsonObject.getAsJsonObject("devDependencies")
        
        assertNull(dependencies)
        assertNull(devDependencies)
    }

    @Test
    fun testDetectsScopedKnipPackage() {
        val packageJson = """
            {
                "name": "test-project",
                "devDependencies": {
                    "@knip/knip": "^5.0.0"
                }
            }
        """.trimIndent()

        val jsonObject = JsonParser.parseString(packageJson).asJsonObject
        val devDependencies = jsonObject.getAsJsonObject("devDependencies")
        
        assertNotNull(devDependencies)
        assertTrue(hasKnipPackage(devDependencies))
    }

    /**
     * Helper method that mirrors the logic in KnipDetectionActivity.
     */
    private fun hasKnipPackage(dependencies: JsonObject): Boolean {
        return KNIP_PACKAGES.any { packageName ->
            dependencies.has(packageName)
        }
    }
}
