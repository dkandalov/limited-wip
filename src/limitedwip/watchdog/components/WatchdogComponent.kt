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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import limitedwip.common.LimitedWipCheckin
import limitedwip.common.TimerComponent
import limitedwip.common.settings.LimitedWIPSettings
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.watchdog.Watchdog

class WatchdogComponent(project: Project) : AbstractProjectComponent(project) {
    private lateinit var watchdog: Watchdog
    private val timer = ApplicationManager.getApplication().getComponent(TimerComponent::class.java)
    private lateinit var ideAdapter: IdeAdapter

    override fun projectOpened() {
        val settings = ServiceManager.getService(LimitedWIPSettings::class.java)
        val changeSizeCalculator = ChangeSizeCalculator(myProject)
        ideAdapter = IdeAdapter(myProject, changeSizeCalculator)
        watchdog = Watchdog(ideAdapter).init(Watchdog.Settings(
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
                    changeSizeCalculator.onTimer()
                }, ModalityState.any())
            }
        }, myProject)

        LimitedWipConfigurable.registerSettingsListener(myProject, LimitedWipConfigurable.Listener { commonSettings ->
            watchdog.onSettings(Watchdog.Settings(
                commonSettings.watchdogEnabled,
                commonSettings.maxLinesInChange,
                commonSettings.notificationIntervalInSeconds(),
                commonSettings.showRemainingChangesInToolbar
            ))
        })

        LimitedWipCheckin.registerListener(myProject, object : LimitedWipCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) {
                watchdog.onCommit()
            }
        })
    }

    fun currentChangeListSize(): Int {
        return ideAdapter.currentChangeListSizeInLines().value
    }

    fun toggleSkipNotificationsUntilCommit() {
        watchdog.toggleSkipNotificationsUntilCommit()
    }

    fun skipNotificationsUntilCommit(value: Boolean) {
        watchdog.skipNotificationsUntilCommit(value)
    }
}
