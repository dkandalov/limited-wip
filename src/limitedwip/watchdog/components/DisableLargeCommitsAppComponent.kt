// Because ApplicationComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.watchdog.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import limitedwip.common.pluginDisplayName
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.vcs.AllowCheckinListener
import limitedwip.common.vcs.openCommitDialog
import limitedwip.common.vcs.registerBeforeCheckInListener

class DisableLargeCommitsAppComponent : ApplicationComponent, LimitedWipConfigurable.Listener {
    private var enabled: Boolean = false
    private var maxChangeSizeInLines: Int = 0
    private var allowCommitOnceWithoutCheck = false

    override fun initComponent() {
        registerBeforeCheckInListener(object: AllowCheckinListener {
            override fun allowCheckIn(project: Project, changes: List<Change>): Boolean {
                if (allowCommitOnceWithoutCheck) {
                    allowCommitOnceWithoutCheck = false
                    return true
                }
                if (!enabled) return true

                val watchdogComponent = project.getComponent(WatchdogComponent::class.java) ?: return true

                val changeSize = watchdogComponent.currentChangeListSize()
                if (changeSize > maxChangeSizeInLines) {
                    notifyThatCommitWasCancelled(project)
                    return false
                }
                return true
            }
        })
        LimitedWipConfigurable.registerSettingsListener(ApplicationManager.getApplication(), this)
    }

    private fun notifyThatCommitWasCancelled(project: Project) {
        val listener = NotificationListener { _, _ ->
            allowCommitOnceWithoutCheck = true
            openCommitDialog()
        }

        val notification = Notification(
            pluginDisplayName,
            pluginDisplayName,
            "Commit was cancelled because change size is above threshold<br/> (<a href=\"\">Click here</a> to force commit anyway)",
            NotificationType.ERROR,
            listener
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    override fun disposeComponent() {}

    override fun getComponentName() = this.javaClass.canonicalName!!

    override fun onSettingsUpdate(settings: LimitedWipSettings) {
        this.enabled = settings.noCommitsAboveThreshold
        this.maxChangeSizeInLines = settings.maxLinesInChange
    }
}
