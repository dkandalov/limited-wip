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
import limitedwip.common.vcs.revertCurrentChangeList
import java.awt.Window

class Ide(
    private val project: Project,
    private var settings: AutoRevert.Settings,
    private val widget: AutoRevertStatusBarWidget
) {
    init {
        Disposer.register(project, Disposable {
            val statusBar = project.statusBar()
            if (statusBar != null) {
                widget.showStoppedText()
                statusBar.removeWidget(widget.ID())
                statusBar.updateWidget(widget.ID())
            }
        })
    }

    fun revertCurrentChangeList(): Int = revertCurrentChangeList(project)

    fun showThatAutoRevertStopped() {
        widget.showStoppedText()
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
            widget.showTimeLeft(formatTime(secondsLeft))
        } else {
            widget.showStartedText()
        }
        updateStatusBar()
    }

    fun showPaused() {
        widget.showPausedText()
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
        val hasAutoRevertWidget = statusBar.getWidget(widget.ID()) != null
        when {
            hasAutoRevertWidget && settings.enabled -> statusBar.updateWidget(widget.ID())
            hasAutoRevertWidget                     -> statusBar.removeWidget(widget.ID())
            settings.enabled                        -> {
                widget.showStoppedText()
                statusBar.addWidget(widget, "before Position")
                statusBar.updateWidget(widget.ID())
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

