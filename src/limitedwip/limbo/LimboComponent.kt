package limitedwip.limbo

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import limitedwip.autorevert.components.IdeAdapter
import limitedwip.common.LimitedWipCheckin

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
    }

    fun amountOfTestsRun(): Int {
        return amountOfTestsRun
    }

    fun resetTestsCounter() {
        amountOfTestsRun = 0
    }
}