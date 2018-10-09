package limitedwip.autorevert.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
import com.intellij.openapi.wm.WindowManager
import limitedwip.autorevert.AutoRevert
import limitedwip.autorevert.ui.AutoRevertStatusBarWidget
import limitedwip.common.PluginId
import java.util.concurrent.atomic.AtomicInteger

class IdeAdapter(private val project: Project) {

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
        val revertedFilesCount = AtomicInteger(0)
        ApplicationManager.getApplication().runWriteAction(Runnable {
            try {

                val changes = ChangeListManager.getInstance(project).defaultChangeList.changes
                revertedFilesCount.set(changes.size)
                if (changes.isEmpty()) return@Runnable

                RollbackWorker(project, "auto-revert", false).doRollback(changes, true, null, null)

                val changedFiles = changes.mapNotNull { change -> change.virtualFile }
                FileDocumentManager.getInstance().reloadFiles(*changedFiles.toTypedArray())

            } catch (e: Exception) {
                // observed exception while reloading project at the time of auto-revert
                logger.error("Error while doing revert", e)
            }
        })
        return revertedFilesCount.get()
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

    fun onChangesRevert() {
        val notification = Notification(
            PluginId.displayName,
            PluginId.displayName,
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

    companion object {
        private val logger = Logger.getInstance(IdeAdapter::class.java)

        private fun Project.statusBar() = WindowManager.getInstance().getStatusBar(this)

        private fun formatTime(seconds: Int): String {
            val min: Int = seconds / 60
            val sec: Int = seconds % 60
            return String.format("%02d", min) + ":" + String.format("%02d", sec)
        }
    }
}

