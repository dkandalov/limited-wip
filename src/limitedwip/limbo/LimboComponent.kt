// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")
package limitedwip.limbo

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import limitedwip.autorevert.components.IdeAdapter
import limitedwip.common.LimitedWipCheckin
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings

class LimboComponent(project: Project) : AbstractProjectComponent(project) {
    private val unitTestsWatcher = UnitTestsWatcher(myProject)
    private val ideAdapter = IdeAdapter(myProject)
    private var amountOfTestsRun = 0

    override fun projectOpened() {
        unitTestsWatcher.start(object : UnitTestsWatcher.Listener {
            override fun onUnitTestSucceeded() {
                amountOfTestsRun++
            }

            override fun onUnitTestFailed() {
                ideAdapter.revertCurrentChangeList()
                resetTestsCounter()
            }
        })

        LimitedWipCheckin.registerListener(myProject, object : LimitedWipCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) {
                resetTestsCounter()
            }
        })

        LimitedWipConfigurable.registerSettingsListener(myProject, object : LimitedWipConfigurable.Listener {
            override fun onSettingsUpdate(settings: LimitedWipSettings) {
                val appComponent = ServiceManager.getService(DisableCommitWithoutRunningTestAppComponent::class.java)
                appComponent.enabled = settings.limboEnabled
            }
        })
    }

    fun amountOfTestsRun(): Int {
        return amountOfTestsRun
    }

    private fun resetTestsCounter() {
        amountOfTestsRun = 0
    }
}