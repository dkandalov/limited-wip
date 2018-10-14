// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.watchdog.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import limitedwip.common.LimitedWipCheckin
import limitedwip.common.TimerComponent
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.watchdog.Watchdog

class WatchdogComponent(project: Project) : AbstractProjectComponent(project) {
    private lateinit var watchdog: Watchdog
    private val timer = ApplicationManager.getApplication().getComponent(TimerComponent::class.java)
    private lateinit var ide: Ide

    override fun projectOpened() {
        val settings = ServiceManager.getService(LimitedWipSettings::class.java)
        val changeSizeWatcher = ChangeSizeWatcher(myProject)
        ide = Ide(myProject, changeSizeWatcher)
        watchdog = Watchdog(ide, Watchdog.Settings(
            settings.watchdogEnabled,
            settings.maxLinesInChange,
            settings.notificationIntervalInSeconds(),
            settings.showRemainingChangesInToolbar
        ))

        timer.addListener(object : TimerComponent.Listener {
            override fun onUpdate(seconds: Int) {
                ApplicationManager.getApplication().invokeLater(Runnable {
                    // Project can be closed (disposed) during handover between timer thread and EDT.
                    if (myProject.isDisposed) return@Runnable
                    watchdog.onTimer(seconds)
                    changeSizeWatcher.onTimer()
                }, ModalityState.any())
            }
        }, myProject)

        LimitedWipConfigurable.registerSettingsListener(myProject, object : LimitedWipConfigurable.Listener {
            override fun onSettingsUpdate(settings: LimitedWipSettings) {
                watchdog.onSettings(Watchdog.Settings(
                    settings.watchdogEnabled,
                    settings.maxLinesInChange,
                    settings.notificationIntervalInSeconds(),
                    settings.showRemainingChangesInToolbar
                ))
            }
        })

        LimitedWipCheckin.registerListener(myProject, object : LimitedWipCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) {
                watchdog.onCommit()
            }
        })
    }

    fun currentChangeListSize(): Int = ide.currentChangeListSizeInLines().value

    fun toggleSkipNotificationsUntilCommit() {
        watchdog.toggleSkipNotificationsUntilCommit()
    }

    fun skipNotificationsUntilCommit(value: Boolean) {
        watchdog.skipNotificationsUntilCommit(value)
    }
}
