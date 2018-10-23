// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.watchdog.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import limitedwip.common.TimerAppComponent
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.settings.toSeconds
import limitedwip.common.vcs.AllowCommitAppComponent
import limitedwip.common.vcs.AllowCommitListener
import limitedwip.common.vcs.SuccessfulCheckin
import limitedwip.watchdog.Watchdog
import limitedwip.watchdog.ui.WatchdogStatusBarWidget

class WatchdogComponent(project: Project): AbstractProjectComponent(project) {
    private val timer = TimerAppComponent.getInstance()
    private val ide: Ide
    private val watchdog: Watchdog
    private val changeSizeWatcher = ChangeSizeWatcher(myProject)

    init {
        val settings = ServiceManager.getService(LimitedWipSettings::class.java).toWatchdogSettings()
        ide = Ide(myProject, changeSizeWatcher, WatchdogStatusBarWidget(), settings)
        watchdog = Watchdog(ide, settings)
    }

    override fun projectOpened() {
        ide.listener = object: Ide.Listener {
            override fun onForceCommit() = watchdog.onForceCommit()
            override fun onSkipNotificationsUntilCommit() = watchdog.onSkipNotificationsUntilCommit()
        }

        timer.addListener(object: TimerAppComponent.Listener {
            override fun onUpdate(seconds: Int) {
                ApplicationManager.getApplication().invokeLater(Runnable {
                    if (myProject.isDisposed) return@Runnable // Project can be closed (disposed) during handover between timer thread and EDT.
                    changeSizeWatcher.calculateCurrentChangeListSizeInLines()
                    watchdog.onTimer(seconds)
                }, ModalityState.any())
            }
        }, myProject)

        LimitedWipConfigurable.registerSettingsListener(myProject, object: LimitedWipConfigurable.Listener {
            override fun onSettingsUpdate(settings: LimitedWipSettings) =
                watchdog.onSettings(settings.toWatchdogSettings())
        })

        SuccessfulCheckin.registerListener(myProject, object: SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) = watchdog.onSuccessfulCommit()
        })
        AllowCommitAppComponent.getInstance().addListener(myProject, object: AllowCommitListener {
            override fun allowCommit(project: Project, changes: List<Change>) =
                project != myProject || watchdog.isCommitAllowed(changeSizeWatcher.getChangeListSizeInLines())
        })
    }

    fun toggleSkipNotificationsUntilCommit() = watchdog.toggleSkipNotificationsUntilCommit()

    private fun LimitedWipSettings.toWatchdogSettings() =
        Watchdog.Settings(
            watchdogEnabled,
            maxLinesInChange,
            notificationIntervalInMinutes.toSeconds(),
            showRemainingChangesInToolbar,
            noCommitsAboveThreshold
        )
}
