package limitedwip.watchdog.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.NotificationType.WARNING
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.wm.WindowManager
import limitedwip.common.pluginDisplayName
import limitedwip.common.vcs.AllowCommit
import limitedwip.common.vcs.doCommitWithoutDialog
import limitedwip.common.vcs.invokeLater
import limitedwip.watchdog.ChangeSize
import limitedwip.watchdog.Watchdog
import limitedwip.watchdog.ui.WatchdogStatusBarWidget

class WatchdogIde(
    private val project: Project,
    private val changeSizeWatcher: ChangeSizeWatcher,
    private val widget: WatchdogStatusBarWidget,
    private var settings: Watchdog.Settings
) {
    lateinit var listener: Listener
    private var lastNotification: Notification? = null
    private var changesInLastCancelledCommit: List<Change>? = null
    private val notificationTitle = "Change size watchdog - $pluginDisplayName"

    init {
        widget.listener = object: WatchdogStatusBarWidget.Listener {
            override fun onClick() = listener.onWidgetClick()
        }
        AllowCommit.addListener(project, object: AllowCommit.Listener {
            override fun allowCommit(project: Project, changes: List<Change>): Boolean {
                val canCommit = project != this@WatchdogIde.project || listener.allowCommit()
                changesInLastCancelledCommit = if (!canCommit) changes else null
                return canCommit
            }
        })
    }

    fun currentChangeListSizeInLines() = changeSizeWatcher.changeListSizeInLines

    fun calculateCurrentChangeListSizeInLines() = changeSizeWatcher.calculateCurrentChangeListSizeInLines()

    fun showCurrentChangeListSize(linesInChange: ChangeSize, maxLinesInChange: Int) {
        widget.showChangeSize(linesInChange.toPrintableString(), maxLinesInChange)
        updateStatusBar()
    }

    fun onSettingsUpdate(settings: Watchdog.Settings) {
        this.settings = settings
        updateStatusBar()
    }

    fun showNotificationThatWatchdogIsDisableUntilNextCommit(value: Boolean) {
        val stateDescription = if (value) "disabled till next commit" else "enabled"
        val notification = Notification(
            pluginDisplayName,
            notificationTitle,
            "Change size notifications $stateDescription",
            INFORMATION
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    fun showNotificationThatChangeSizeIsTooBig(linesChanged: ChangeSize, changedLinesLimit: Int) {
        val notification = Notification(
            pluginDisplayName,
            notificationTitle,
            "There are too many changes. " +
                "Lines changed: " + linesChanged.toPrintableString() + ", threshold: " + changedLinesLimit + ". " +
                "Please commit or revert some of the changes. To skip notifications till next commit <a href=\"\">click here</a>.",
            WARNING
        ) { notification, _ ->
            notification.expire()
            listener.onSkipNotificationsUntilCommit()
        }
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)

        lastNotification?.expire()
        lastNotification = notification
    }

    fun hideNotificationThatChangeSizeIsTooBig() {
        lastNotification?.expire()
        lastNotification = null
    }

    fun notifyThatCommitWasCancelled() {
        val notification = Notification(
            pluginDisplayName,
            notificationTitle,
            "Commit was cancelled because change size is above threshold. To force commit anyway <a href=\"\">click here</a>.",
            WARNING
        ) { notification, _ ->
            notification.expire()
            listener.onForceCommit()
        }
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)

        lastNotification?.expire()
        lastNotification = notification
    }

    fun commitWithoutDialog() {
        invokeLater {
            doCommitWithoutDialog(project)
        }
    }

    private fun updateStatusBar() {
        val statusBar = WindowManager.getInstance().getStatusBar(project) ?: return
        val hasWatchdogWidget = statusBar.getWidget(widget.ID()) != null
        val shouldShowWatchdog = settings.enabled && settings.showRemainingChangesInToolbar
        when {
            hasWatchdogWidget && shouldShowWatchdog  -> widget.updateOn(statusBar)
            hasWatchdogWidget && !shouldShowWatchdog -> widget.removeFrom(statusBar)
            !hasWatchdogWidget && shouldShowWatchdog -> widget.addTo(statusBar)
        }
    }

    private fun ChangeSize.toPrintableString() =
        if (this == ChangeSize.NA) "-"
        else (if (isApproximate) "≈" else "") + value.toString()

    interface Listener {
        fun onForceCommit()
        fun onSkipNotificationsUntilCommit()
        fun onWidgetClick()
        fun allowCommit(): Boolean
    }
}
