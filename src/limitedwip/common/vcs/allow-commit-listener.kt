package limitedwip.common.vcs

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.*
import com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.CANCEL
import com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.COMMIT
import limitedwip.common.ifNotNull
import java.util.concurrent.CopyOnWriteArraySet

object AllowCommit: CheckinHandlerFactory() {
    val listeners = CopyOnWriteArraySet<Listener>()

    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler =
        object: CheckinHandler(), CheckinMetaHandler {
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

    fun addListener(parentDisposable: Disposable, listener: Listener) {
        Disposer.register(parentDisposable) { listeners.remove(listener) }
        listeners.add(listener)
    }

    interface Listener {
        fun allowCommit(project: Project, changes: List<Change>): Boolean
    }
}
