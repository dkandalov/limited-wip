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
import java.awt.Window

class Ide(
    private val project: Project,
    private var settings: AutoRevert.Settings,
    private val autoRevertWidget: AutoRevertStatusBarWidget
) {
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
        return limitedwip.common.vcs.revertCurrentChangeList(project)
    }

    fun showThatAutoRevertStopped() {
        autoRevertWidget.showStoppedText()
        updateStatusBar()
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

    fun showTimeTillRevert(secondsLeft: Int) {
        if (settings.showTimerInToolbar) {
            autoRevertWidget.showTimeLeft(formatTime(secondsLeft))
        } else {
            autoRevertWidget.showStartedText()
        }
        updateStatusBar()
    }

    fun onSettingsUpdate(settings: AutoRevert.Settings) {
        this.settings = settings
        updateStatusBar()
    }

    fun isCommitDialogOpen(): Boolean {
        val projectFrame = WindowManager.getInstance().getFrame(project) ?: return false
        return Window.getWindows()
            .filter { it.isVisible && it.parent == projectFrame }
            .any {
                val s = it.toString()
                s.contains("com.intellij.openapi.ui.impl.DialogWrapperPeerImpl\$MyDialog") && s.contains("title=Commit Changes")
            }
    }

    private fun updateStatusBar() {
        val statusBar = project.statusBar() ?: return
        val hasAutoRevertWidget = statusBar.getWidget(autoRevertWidget.ID()) != null
        when {
            hasAutoRevertWidget && settings.autoRevertEnabled -> statusBar.updateWidget(autoRevertWidget.ID())
            hasAutoRevertWidget                               -> statusBar.removeWidget(autoRevertWidget.ID())
            settings.autoRevertEnabled                        -> {
                autoRevertWidget.showStoppedText()
                statusBar.addWidget(autoRevertWidget, "before Position")
                statusBar.updateWidget(autoRevertWidget.ID())
            }
        }
    }

    private fun Project.statusBar() = WindowManager.getInstance().getStatusBar(this)

    private fun formatTime(seconds: Int): String {
        val min: Int = seconds / 60
        val sec: Int = seconds % 60
        return String.format("%02d", min) + ":" + String.format("%02d", sec)
    }
}

