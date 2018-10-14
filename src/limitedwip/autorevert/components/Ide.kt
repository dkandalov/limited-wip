package limitedwip.autorevert.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.WindowManager
import limitedwip.autorevert.AutoRevert
import limitedwip.autorevert.ui.AutoRevertStatusBarWidget
import limitedwip.common.pluginDisplayName

class Ide(private val project: Project) {

    private val autoRevertWidget = AutoRevertStatusBarWidget()
    private var settings: AutoRevert.Settings? = null

    init {
        Disposer.register(project, Disposable {
            val statusBar = project.statusBar()
            if (statusBar != null) {
                autoRevertWidget.showStoppedText()
                statusBar.removeWidget(autoRevertWidget.ID())
                statusBar.updateWidget(autoRevertWidget.ID())
            }
        })
    }

    fun revertCurrentChangeList(): Int {
        return limitedwip.common.revertCurrentChangeList(project)
    }

    fun onAutoRevertStarted(timeEventsTillRevert: Int) {
        if (settings!!.showTimerInToolbar) {
            autoRevertWidget.showTime(formatTime(timeEventsTillRevert))
        } else {
            autoRevertWidget.showStartedText()
        }
        updateStatusBar()
    }

    fun onAutoRevertStopped() {
        autoRevertWidget.showStoppedText()
        updateStatusBar()
    }

    fun showNotificationThatChangesWereReverted() {
        val notification = Notification(
            pluginDisplayName,
            pluginDisplayName,
            "Current changelist was reverted",
            NotificationType.WARNING
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    fun onCommit(timeEventsTillRevert: Int) {
        if (settings!!.showTimerInToolbar) {
            autoRevertWidget.showTime(formatTime(timeEventsTillRevert))
        } else {
            autoRevertWidget.showStartedText()
        }
        updateStatusBar()
    }

    fun onTimeTillRevert(secondsLeft: Int) {
        if (settings!!.showTimerInToolbar) {
            autoRevertWidget.showTime(formatTime(secondsLeft))
        } else {
            autoRevertWidget.showStartedText()
        }
        updateStatusBar()
    }

    fun onSettingsUpdate(settings: AutoRevert.Settings) {
        this.settings = settings
        updateStatusBar()
    }

    private fun updateStatusBar() {
        val statusBar = project.statusBar() ?: return

        val hasAutoRevertWidget = statusBar.getWidget(autoRevertWidget.ID()) != null
        if (hasAutoRevertWidget && settings!!.autoRevertEnabled) {
            statusBar.updateWidget(autoRevertWidget.ID())

        } else if (hasAutoRevertWidget) {
            statusBar.removeWidget(autoRevertWidget.ID())

        } else if (settings!!.autoRevertEnabled) {
            autoRevertWidget.showStoppedText()
            statusBar.addWidget(autoRevertWidget, "before Position")
            statusBar.updateWidget(autoRevertWidget.ID())
        }
    }

    private fun Project.statusBar() = WindowManager.getInstance().getStatusBar(this)

    private fun formatTime(seconds: Int): String {
        val min: Int = seconds / 60
        val sec: Int = seconds % 60
        return String.format("%02d", min) + ":" + String.format("%02d", sec)
    }
}

