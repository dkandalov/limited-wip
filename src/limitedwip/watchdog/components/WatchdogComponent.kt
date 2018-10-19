// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.watchdog.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import limitedwip.common.TimerAppComponent
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.vcs.SuccessfulCheckin
import limitedwip.watchdog.Watchdog
import limitedwip.watchdog.ui.WatchdogStatusBarWidget

class WatchdogComponent(project: Project): AbstractProjectComponent(project) {
    private lateinit var watchdog: Watchdog
    private val timer = ApplicationManager.getApplication().getComponent(TimerAppComponent::class.java)
    private lateinit var ide: Ide

    override fun projectOpened() {
        val settings = ServiceManager.getService(LimitedWipSettings::class.java).toWatchdogSettings()
        val changeSizeWatcher = ChangeSizeWatcher(myProject)
        ide = Ide(myProject, changeSizeWatcher, WatchdogStatusBarWidget(), settings)
        watchdog = Watchdog(ide, settings)

        timer.addListener(object: TimerAppComponent.Listener {
            override fun onUpdate(seconds: Int) {
                ApplicationManager.getApplication().invokeLater(Runnable {
                    // Project can be closed (disposed) during handover between timer thread and EDT.
                    if (myProject.isDisposed) return@Runnable
                    watchdog.onTimer(seconds)
                    changeSizeWatcher.onTimer()
                }, ModalityState.any())
            }
        }, myProject)

        LimitedWipConfigurable.registerSettingsListener(myProject, object: LimitedWipConfigurable.Listener {
            override fun onSettingsUpdate(settings: LimitedWipSettings) {
                watchdog.onSettings(settings.toWatchdogSettings())
            }
        })

        SuccessfulCheckin.registerListener(myProject, object: SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) {
                watchdog.onCommit()
            }
        })
    }

    private fun LimitedWipSettings.toWatchdogSettings() =
        Watchdog.Settings(
            watchdogEnabled,
            maxLinesInChange,
            notificationIntervalInSeconds(),
            showRemainingChangesInToolbar
        )

    fun currentChangeListSize(): Int = ide.currentChangeListSizeInLines().value

    fun toggleSkipNotificationsUntilCommit() {
        watchdog.toggleSkipNotificationsUntilCommit()
    }

    fun skipNotificationsUntilCommit(value: Boolean) {
        watchdog.skipNotificationsUntilCommit(value)
    }
}
