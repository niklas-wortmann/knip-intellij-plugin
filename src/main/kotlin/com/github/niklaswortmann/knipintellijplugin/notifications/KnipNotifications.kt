package com.github.niklaswortmann.knipintellijplugin.notifications

import com.github.niklaswortmann.knipintellijplugin.KnipBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * Helper object for displaying Knip-related notifications to the user.
 */
object KnipNotifications {

    private const val NOTIFICATION_GROUP_ID = "Knip"

    /**
     * Show an info notification.
     */
    fun info(project: Project, content: String, title: String = "Knip") {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID) ?: return
        notificationGroup
            .createNotification(title, content, NotificationType.INFORMATION)
            .notify(project)
    }

    /**
     * Show a warning notification.
     */
    fun warning(project: Project, content: String, title: String = "Knip") {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID) ?: return
        notificationGroup
            .createNotification(title, content, NotificationType.WARNING)
            .notify(project)
    }

    /**
     * Show an error notification.
     */
    fun error(project: Project, content: String, title: String = "Knip") {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID) ?: return
        notificationGroup
            .createNotification(title, content, NotificationType.ERROR)
            .notify(project)
    }

    /**
     * Show server error notification.
     */
    fun serverError(project: Project, errorMessage: String) {
        error(project, KnipBundle.message("serverError", errorMessage))
    }

    /**
     * Show Node.js not found notification with helpful message.
     */
    fun nodeNotFound(project: Project) {
        warning(
            project,
            "Node.js/npx not found. Please install Node.js or configure the path in Settings > Tools > Knip.",
            "Knip: Node.js Required"
        )
    }

    /**
     * Show notification when Knip language server package is not installed.
     */
    fun knipServerNotInstalled(project: Project) {
        warning(
            project,
            "The @knip/language-server package could not be found. Run 'npm install -g @knip/language-server' or ensure it's available via npx.",
            "Knip: Language Server Not Found"
        )
    }
}
