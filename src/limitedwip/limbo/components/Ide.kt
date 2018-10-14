package limitedwip.limbo.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import limitedwip.autorevert.components.Ide
import limitedwip.common.pluginDisplayName
import limitedwip.limbo.Limbo

class Ide(private val project: Project, private val ideFromAutoRevert: Ide) {
    lateinit var limbo: Limbo

    fun revertCurrentChangeList() {
        ideFromAutoRevert.revertCurrentChangeList()
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
}