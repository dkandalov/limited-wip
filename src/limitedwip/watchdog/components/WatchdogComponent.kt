// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.watchdog.components

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import limitedwip.common.TimerAppComponent
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.settings.toSeconds
import limitedwip.common.toPathMatchers
import limitedwip.common.vcs.SuccessfulCheckin
import limitedwip.common.vcs.invokeLater
import limitedwip.watchdog.Watchdog
import limitedwip.watchdog.ui.WatchdogStatusBarWidget

class WatchdogComponent(project: Project): AbstractProjectComponent(project) {
    private val timer = TimerAppComponent.getInstance()
    @Volatile private var enabled = false

    override fun projectOpened() {
        val settings = LimitedWipSettings.getInstance(myProject).toWatchdogSettings()
        val ide = Ide(myProject, ChangeSizeWatcher(myProject), WatchdogStatusBarWidget(), settings)
        val watchdog = Watchdog(ide, settings)
        enabled = settings.enabled

        ide.listener = object: Ide.Listener {
            override fun allowCommit() = watchdog.isCommitAllowed(ide.currentChangeListSizeInLines())
            override fun onForceCommit() = watchdog.onForceCommit()
            override fun onSkipNotificationsUntilCommit() = watchdog.onSkipNotificationsUntilCommit()
            override fun onWidgetClick() = watchdog.toggleSkipNotificationsUntilCommit()
        }

        timer.addListener(myProject, object: TimerAppComponent.Listener {
            override fun onUpdate() {
                // Optimisation to avoid scheduling task when component is not enabled.
                if (!enabled) return

                invokeLater(ModalityState.any()) {
                    // Project can be closed (disposed) during handover between timer thread and EDT.
                    if (myProject.isDisposed) return@invokeLater
                    watchdog.onTimer()
                }
            }
        })

        LimitedWipSettings.getInstance(myProject).addListener(myProject, object: LimitedWipSettings.Listener {
            override fun onUpdate(settings: LimitedWipSettings) {
                enabled = settings.watchdogEnabled
                watchdog.onSettingsUpdate(settings.toWatchdogSettings())
            }
        })

        SuccessfulCheckin.registerListener(myProject, object: SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allChangesAreCommitted: Boolean) = watchdog.onSuccessfulCommit()
        })
    }

    private fun LimitedWipSettings.toWatchdogSettings() =
        Watchdog.Settings(
            watchdogEnabled,
            maxLinesInChange,
            notificationIntervalInMinutes.toSeconds(),
            showRemainingChangesInToolbar,
            noCommitsAboveThreshold,
            exclusions.toPathMatchers()
        )
}
