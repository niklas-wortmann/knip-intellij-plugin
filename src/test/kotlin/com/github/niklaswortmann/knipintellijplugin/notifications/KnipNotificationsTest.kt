package com.github.niklaswortmann.knipintellijplugin.notifications

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Unit tests for KnipNotifications.
 * Note: These tests verify that notification methods can be called without errors.
 * Actual notification display is tested via UI tests.
 */
class KnipNotificationsTest : BasePlatformTestCase() {

    fun testInfoNotification() {
        // Should not throw exception
        KnipNotifications.info(project, "Test info message")
        assertTrue(true)
    }

    fun testInfoNotificationWithCustomTitle() {
        // Should not throw exception
        KnipNotifications.info(project, "Test info message", "Custom Title")
        assertTrue(true)
    }

    fun testWarningNotification() {
        // Should not throw exception
        KnipNotifications.warning(project, "Test warning message")
        assertTrue(true)
    }

    fun testWarningNotificationWithCustomTitle() {
        // Should not throw exception
        KnipNotifications.warning(project, "Test warning message", "Custom Warning")
        assertTrue(true)
    }

    fun testErrorNotification() {
        // Should not throw exception
        KnipNotifications.error(project, "Test error message")
        assertTrue(true)
    }

    fun testErrorNotificationWithCustomTitle() {
        // Should not throw exception
        KnipNotifications.error(project, "Test error message", "Custom Error")
        assertTrue(true)
    }

    fun testServerStartedNotification() {
        // Should not throw exception
        KnipNotifications.serverStarted(project)
        assertTrue(true)
    }

    fun testServerStoppedNotification() {
        // Should not throw exception
        KnipNotifications.serverStopped(project)
        assertTrue(true)
    }

    fun testServerErrorNotification() {
        // Should not throw exception
        KnipNotifications.serverError(project, "Connection failed")
        assertTrue(true)
    }

    fun testNodeNotFoundNotification() {
        // Should not throw exception
        KnipNotifications.nodeNotFound(project)
        assertTrue(true)
    }

    fun testKnipServerNotInstalledNotification() {
        // Should not throw exception
        KnipNotifications.knipServerNotInstalled(project)
        assertTrue(true)
    }
}
