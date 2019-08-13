package limitedwip.watchdog.components

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import limitedwip.common.TimerAppComponent
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.settings.toSeconds
import limitedwip.common.toPathMatchers
import limitedwip.common.vcs.SuccessfulCheckin
import limitedwip.common.vcs.invokeLater
import limitedwip.watchdog.Watchdog
import limitedwip.watchdog.ui.WatchdogStatusBarWidget

class WatchdogComponentStartup: StartupActivity {
    override fun runActivity(project: Project) = WatchdogComponent(project).start()
}

class WatchdogComponent(private val project: Project) {
    @Volatile private var enabled = false

    fun start() {
        val settings = LimitedWipSettings.getInstance(project).toWatchdogSettings()
        val ide = Ide(project, ChangeSizeWatcher(project), WatchdogStatusBarWidget(), settings)
        val watchdog = Watchdog(ide, settings)
        enabled = settings.enabled

        ide.listener = object: Ide.Listener {
            override fun allowCommit() = watchdog.isCommitAllowed(ide.currentChangeListSizeInLines())
            override fun onForceCommit() = watchdog.onForceCommit()
            override fun onSkipNotificationsUntilCommit() = watchdog.onSkipNotificationsUntilCommit()
            override fun onWidgetClick() = watchdog.toggleSkipNotificationsUntilCommit()
        }

        TimerAppComponent.getInstance().addListener(project, object: TimerAppComponent.Listener {
            override fun onUpdate() {
                // Optimisation to avoid scheduling task when component is not enabled.
                if (!enabled) return

                invokeLater(ModalityState.any()) {
                    // Project can be closed (disposed) during handover between timer thread and EDT.
                    if (project.isDisposed) return@invokeLater
                    watchdog.onTimer()
                }
            }
        })

        LimitedWipSettings.getInstance(project).addListener(project, object: LimitedWipSettings.Listener {
            override fun onUpdate(settings: LimitedWipSettings) {
                enabled = settings.watchdogEnabled
                watchdog.onSettingsUpdate(settings.toWatchdogSettings())
            }
        })

        SuccessfulCheckin.registerListener(project, object: SuccessfulCheckin.Listener {
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
