package limitedwip.limbo

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import limitedwip.autorevert.components.IdeAdapter

class LimboComponent(private val project: Project) : AbstractProjectComponent(project) {
    private val unitTestsWatcher = UnitTestsWatcher(project)
    private val ideAdapter = IdeAdapter(project)
    private var amountOfTestsRun = 0

    override fun projectOpened() {
        unitTestsWatcher.start(object : UnitTestsWatcher.Listener {
            override fun onUnitTestSucceeded() {
                amountOfTestsRun++
            }

            override fun onUnitTestFailed() {
                ideAdapter.revertCurrentChangeList()
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