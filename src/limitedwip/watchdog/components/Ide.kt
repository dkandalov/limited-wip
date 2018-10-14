package limitedwip.watchdog.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.WindowManager
import limitedwip.common.pluginDisplayName
import limitedwip.watchdog.ChangeSize
import limitedwip.watchdog.Watchdog
import limitedwip.watchdog.ui.WatchdogStatusBarWidget

class Ide(private val project: Project, private val changeSizeWatcher: ChangeSizeWatcher) {
    private val watchdogWidget = WatchdogStatusBarWidget()

    private var settings: Watchdog.Settings? = null
    private var lastNotification: Notification? = null

    fun currentChangeListSizeInLines() = changeSizeWatcher.currentChangeListSizeInLines()

    fun showCurrentChangeListSize(linesInChange: ChangeSize, maxLinesInChange: Int) {
        watchdogWidget.showChangeSize(asString(linesInChange), maxLinesInChange)
        updateStatusBar()
    }

    fun onSettingsUpdate(settings: Watchdog.Settings) {
        this.settings = settings
        updateStatusBar()
    }

    fun onSkipNotificationUntilCommit(value: Boolean) {
        val stateDescription = if (value) "disabled till next commit" else "enabled"
        val notification = Notification(
            pluginDisplayName,
            pluginDisplayName,
            "Change size notifications are $stateDescription",
            NotificationType.INFORMATION
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    private fun updateStatusBar() {
        val statusBar = statusBarFor(project) ?: return

        val hasWatchdogWidget = statusBar.getWidget(watchdogWidget.ID()) != null
        val shouldShowWatchdog = settings!!.enabled && settings!!.showRemainingChangesInToolbar
        if (hasWatchdogWidget && shouldShowWatchdog) {
            statusBar.updateWidget(watchdogWidget.ID())

        } else if (hasWatchdogWidget) {
            statusBar.removeWidget(watchdogWidget.ID())

        } else if (shouldShowWatchdog) {
            watchdogWidget.showInitialText(settings!!.maxLinesInChange)
            statusBar.addWidget(watchdogWidget, "before Position")
            statusBar.updateWidget(watchdogWidget.ID())
        }
    }

    private fun statusBarFor(project: Project): StatusBar? {
        return WindowManager.getInstance().getStatusBar(project)
    }

    fun onChangeSizeTooBig(linesChanged: ChangeSize, changedLinesLimit: Int) {
        val listener = NotificationListener { notification, _ ->
            val watchdogComponent = project.getComponent(WatchdogComponent::class.java) ?: return@NotificationListener
            watchdogComponent.skipNotificationsUntilCommit(true)
            notification.expire()
        }

        val notification = Notification(
            pluginDisplayName,
            "Change Size Exceeded Limit",
            "Lines changed: " + asString(linesChanged) + "; " +
                "limit: " + changedLinesLimit + "<br/>" +
                "Please commit, split or revert changes<br/>" +
                "(<a href=\"\">Click here</a> to skip notifications till next commit)",
            NotificationType.WARNING,
            listener
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)

        if (lastNotification != null && !lastNotification!!.isExpired) {
            lastNotification!!.expire()
        }
        lastNotification = notification
    }

    fun onChangeSizeWithinLimit() {
        if (lastNotification != null && !lastNotification!!.isExpired) {
            lastNotification!!.expire()
            lastNotification = null
        }
    }

    private fun asString(changeSize: ChangeSize): String {
        return if (changeSize.isApproximate) "≈" + changeSize.value else changeSize.value.toString()
    }
}
