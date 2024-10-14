package limitedwip.watchdog.components

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vcs.changes.Change
import limitedwip.common.TimerAppComponent
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.settings.TimeUnit.Minutes
import limitedwip.common.toPathMatchers
import limitedwip.common.vcs.AllowCommit
import limitedwip.common.vcs.SuccessfulCheckin
import limitedwip.common.vcs.invokeLater
import limitedwip.common.vcs.registerSuccessfulCheckinListener
import limitedwip.watchdog.Watchdog
import limitedwip.watchdog.ui.WatchdogStatusBarWidget

class WatchdogComponentStartup : ProjectActivity {
    override suspend fun execute(project: Project) = WatchdogComponent(project).start()
}

class WatchdogComponent(private val project: Project) {
    @Volatile private var enabled = false

    fun start() {
        val settings = LimitedWipSettings.getInstance(project).toWatchdogSettings()
        val ide = WatchdogIde(project, ChangeSizeWatcher(project), WatchdogStatusBarWidget(project), settings)
        val watchdog = Watchdog(ide, settings)
        enabled = settings.enabled

        ide.listener = object : WatchdogIde.Listener {
            override fun onForceCommit() = watchdog.onForceCommit()
            override fun onSkipNotificationsUntilCommit() = watchdog.onSkipNotificationsUntilCommit()
            override fun onWidgetClick() = watchdog.toggleSkipNotificationsUntilCommit()
        }

        AllowCommit.addListener(project, object : AllowCommit.Listener {
            override fun allowCommit(project: Project, changes: List<Change>) =
                project != this@WatchdogComponent.project || watchdog.isCommitAllowed(ide.currentChangeListSizeInLines())
        })

        TimerAppComponent.getInstance().addListener(project, object : TimerAppComponent.Listener {
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

        LimitedWipSettings.getInstance(project).addListener(project, object : LimitedWipSettings.Listener {
            override fun onUpdate(settings: LimitedWipSettings) {
                enabled = settings.watchdogEnabled
                watchdog.onSettingsUpdate(settings.toWatchdogSettings())
            }
        })

        registerSuccessfulCheckinListener(project, object : SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allChangesAreCommitted: Boolean) = watchdog.onSuccessfulCommit()
        })
    }

    private fun LimitedWipSettings.toWatchdogSettings() =
        Watchdog.Settings(
            watchdogEnabled,
            maxLinesInChange,
            Minutes.toSeconds(notificationIntervalInMinutes),
            showRemainingChangesInToolbar,
            noCommitsAboveThreshold,
            exclusions.toPathMatchers()
        )
}
