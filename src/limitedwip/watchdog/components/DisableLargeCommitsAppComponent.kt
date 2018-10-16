// Because ApplicationComponent and dataContextFromFocus were deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.watchdog.components

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
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.actions.CommonCheckinProjectAction
import com.intellij.openapi.vcs.changes.Change
import limitedwip.common.pluginDisplayName
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.vcs.AllowCheckinListener
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
            val succeeded = showCommitDialog(0)
            if (!succeeded) {
                allowCommitOnceWithoutCheck = false
            }
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
        this.enabled = settings.disableCommitsAboveThreshold
        this.maxChangeSizeInLines = settings.maxLinesInChange
    }

    companion object {
        private const val maxShowCommitDialogAttempts = 3

        /**
         * Use retrying logic because of UI deadlock (not sure why exactly this happened):
         * "AWT-EventQueue-1 14.0.2#IU-139.658.4, eap:true" prio=0 tid=0x0 nid=0x0 waiting on condition
         * java.lang.Thread.State: WAITING on com.intellij.util.concurrency.Semaphore$Sync@4071d2c1
         * at sun.misc.Unsafe.park(Native Method)
         * at java.util.concurrent.locks.LockSupport.park(LockSupport.java:156)
         * at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:811)
         * at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedInterruptibly(AbstractQueuedSynchronizer.java:969)
         * at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireSharedInterruptibly(AbstractQueuedSynchronizer.java:1281)
         * at com.intellij.util.concurrency.Semaphore.waitForUnsafe(Semaphore.java:74)
         * at com.intellij.openapi.util.ActionCallback.waitFor(ActionCallback.java:269)
         * at com.intellij.openapi.util.AsyncResult.getResultSync(AsyncResult.java:147)
         * at com.intellij.openapi.util.AsyncResult.getResultSync(AsyncResult.java:142)
         * at limitedwip.watchdog.components.DisableLargeCommitsAppComponent$2.hyperlinkUpdate(DisableLargeCommitsAppComponent.java:65)
         * at com.intellij.notification.impl.ui.NotificationsUtil$1.hyperlinkUpdate(NotificationsUtil.java:75)
         */
        private fun showCommitDialog(showCommitDialogAttempts: Int): Boolean {
            if (showCommitDialogAttempts > maxShowCommitDialogAttempts) return false

            val dataContext = DataManager.getInstance().dataContextFromFocus.getResultSync(500)
                ?: return showCommitDialog(showCommitDialogAttempts + 1)

            val actionEvent = AnActionEvent(null,
                dataContext,
                ActionPlaces.UNKNOWN,
                Presentation(),
                ActionManager.getInstance(),
                0
            )
            CommonCheckinProjectAction().actionPerformed(actionEvent)

            return true
        }
    }
}
