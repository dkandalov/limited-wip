package limitedwip.common.vcs

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.CANCEL
import com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.COMMIT
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.vcs.checkin.CheckinMetaHandler
import limitedwip.common.ifNotNull
import java.util.concurrent.CopyOnWriteArraySet

class AllowCommit : CheckinHandlerFactory() {

    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler =
        object : CheckinHandler(), CheckinMetaHandler {
            private var result: ReturnResult? = null

            override fun beforeCheckin(): ReturnResult {
                result.ifNotNull {
                    result = null
                    return it
                }
                return canCommit()
            }

            override fun runCheckinHandlers(runnable: Runnable) {
                // Check if can commit here because CheckinMetaHandlers are called in reverse order
                // and this function will be called before optimising imports (see https://github.com/dkandalov/limited-wip/issues/39)
                result = canCommit()
                runnable.run()
            }

            private fun canCommit(): ReturnResult {
                val canCommit = listeners.all { it.allowCommit(panel.project, panel.selectedChanges.toList()) }
                return if (canCommit) COMMIT else CANCEL
            }
        }

    interface Listener {
        fun allowCommit(project: Project, changes: List<Change>): Boolean
    }

    companion object {
        val listeners = CopyOnWriteArraySet<Listener>()

        fun addListener(parentDisposable: Disposable, listener: Listener) {
            Disposer.register(parentDisposable) { listeners.remove(listener) }
            listeners.add(listener)
        }
    }
}
