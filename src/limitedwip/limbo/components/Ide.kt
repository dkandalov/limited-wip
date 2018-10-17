package limitedwip.limbo.components

import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import limitedwip.common.pluginDisplayName


class Ide(private val project: Project) {
    lateinit var listener: Listener

    fun revertCurrentChangeList() {
        limitedwip.common.vcs.revertCurrentChangeList(project)
    }

    fun notifyThatCommitWasCancelled() {
        val notification = Notification(
            pluginDisplayName,
            pluginDisplayName,
            "Commit was cancelled because no tests were run<br/> " +
            "(<a href=\"\">Click here</a> to force commit anyway)",
            NotificationType.WARNING,
            NotificationListener { _, _ ->
                listener.onForceCommit()
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

    fun openCommitDialog() {
        val application = ApplicationManager.getApplication()
        application.invokeAndWait {
            application.runWriteAction {
                val actionManager = ActionManager.getInstance()
                val dataContext = DataManager.getInstance().getDataContext(IdeFocusManager.getGlobalInstance().focusOwner)
                val anActionEvent = AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, Presentation(), actionManager, 0)
                actionManager.getAction("CheckinProject").actionPerformed(anActionEvent)
            }
        }
    }

    interface Listener {
        fun onForceCommit()
    }
}