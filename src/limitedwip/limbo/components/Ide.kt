package limitedwip.limbo.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import limitedwip.common.pluginDisplayName
import limitedwip.limbo.Limbo

class Ide(private val project: Project) {
    lateinit var limbo: Limbo

    fun revertCurrentChangeList() {
        limitedwip.common.vcs.revertCurrentChangeList(project)
    }

    fun notifyThatCommitWasCancelled() {
        val notification = Notification(
            pluginDisplayName,
            pluginDisplayName,
            "Commit was cancelled because no tests were run<br/> (<a href=\"\">Click here</a> to force commit anyway)",
            NotificationType.ERROR,
            NotificationListener { _, _ ->
                limbo.allowOneCommitWithoutChecks()
            }
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    fun notifyThatChangesWereReverted() {
        val notification = Notification(
            pluginDisplayName,
            pluginDisplayName,
            "Current changelist was reverted",
            NotificationType.WARNING
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }
}