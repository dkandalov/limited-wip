/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package limitedwip.watchdog.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.WindowManager
import limitedwip.common.PluginId
import limitedwip.watchdog.ChangeSize
import limitedwip.watchdog.Watchdog
import limitedwip.watchdog.ui.WatchdogStatusBarWidget

class IdeAdapter(private val project: Project, private val changeSizeCalculator: ChangeSizeCalculator) {
    private val watchdogWidget = WatchdogStatusBarWidget()

    private var settings: Watchdog.Settings? = null
    private var lastNotification: Notification? = null

    fun currentChangeListSizeInLines() = changeSizeCalculator.currentChangeListSizeInLines()

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
            PluginId.displayName,
            PluginId.displayName,
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
        val listener = NotificationListener { notification, event ->
            val watchdogComponent = project.getComponent(WatchdogComponent::class.java) ?: return@NotificationListener
            watchdogComponent.skipNotificationsUntilCommit(true)
            notification.expire()
        }

        val notification = Notification(
            PluginId.displayName,
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
