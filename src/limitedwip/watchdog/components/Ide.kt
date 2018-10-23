package limitedwip.watchdog.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import limitedwip.common.pluginDisplayName
import limitedwip.watchdog.ChangeSize
import limitedwip.watchdog.Watchdog
import limitedwip.watchdog.ui.WatchdogStatusBarWidget

class Ide(
    private val project: Project,
    private val changeSizeWatcher: ChangeSizeWatcher,
    private val watchdogWidget: WatchdogStatusBarWidget,
    private var settings: Watchdog.Settings
) {
    var listener: Listener? = null
    private var lastNotification: Notification? = null

    fun currentChangeListSizeInLines() = changeSizeWatcher.getChangeListSizeInLines()

    fun calculateCurrentChangeListSizeInLines() = changeSizeWatcher.calculateCurrentChangeListSizeInLines()

    fun showCurrentChangeListSize(linesInChange: ChangeSize, maxLinesInChange: Int) {
        watchdogWidget.showChangeSize(linesInChange.toPrintableString(), maxLinesInChange)
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
            pluginDisplayName,
            "Change size notifications are $stateDescription",
            NotificationType.INFORMATION
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    fun showNotificationThatChangeSizeIsTooBig(linesChanged: ChangeSize, changedLinesLimit: Int) {
        val notification = Notification(
            pluginDisplayName,
            "Change Size Exceeded Limit",
            "Lines changed: " + linesChanged.toPrintableString() + "; " +
                "limit: " + changedLinesLimit + "<br/>" +
                "Please commit, split or revert changes<br/>" +
                "(<a href=\"\">Click here</a> to skip notifications till next commit)",
            NotificationType.WARNING,
            NotificationListener { notification, _ ->
                listener?.onSkipNotificationsUntilCommit()
                notification.expire()
            }
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)

        lastNotification.ifNotNull { it.expire() }
        lastNotification = notification
    }

    fun hideNotificationThatChangeSizeIsTooBig() {
        lastNotification.ifNotNull { it.expire() }
        lastNotification = null
    }

    fun notifyThatCommitWasCancelled() {
        val notification = Notification(
            pluginDisplayName,
            pluginDisplayName,
            "Commit was cancelled because change size is above threshold<br/> (<a href=\"\">Click here</a> to force commit anyway)",
            NotificationType.ERROR,
            NotificationListener { _, _ -> listener?.onForceCommit() }
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    fun openCommitDialog() {
        limitedwip.common.vcs.openCommitDialog()
    }

    private fun updateStatusBar() {
        val statusBar = WindowManager.getInstance().getStatusBar(project) ?: return

        val hasWatchdogWidget = statusBar.getWidget(watchdogWidget.ID()) != null
        val shouldShowWatchdog = settings.enabled && settings.showRemainingChangesInToolbar
        if (hasWatchdogWidget && shouldShowWatchdog) {
            statusBar.updateWidget(watchdogWidget.ID())

        } else if (hasWatchdogWidget) {
            statusBar.removeWidget(watchdogWidget.ID())

        } else if (shouldShowWatchdog) {
            watchdogWidget.showInitialText(settings.maxLinesInChange)
            statusBar.addWidget(watchdogWidget, "before Position")
            statusBar.updateWidget(watchdogWidget.ID())
        }
    }

    private fun ChangeSize.toPrintableString() =
        if (isApproximate) "≈$value" else value.toString()

    private fun <T> T?.ifNotNull(f: (T) -> Unit) {
        if (this != null) f(this)
    }

    interface Listener {
        fun onForceCommit()
        fun onSkipNotificationsUntilCommit()
    }
}
