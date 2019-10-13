package limitedwip.autorevert.components

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.InvokeAfterUpdateMode
import com.intellij.util.messages.MessageBusConnection

class RollbackListener(private val project: Project, private val onRollback: (Boolean) -> Unit) {
    private var busConnection: MessageBusConnection? = null

    fun enable() {
        if (busConnection != null) return

        val changeListManager = ChangeListManager.getInstance(project)
        val listener = object: AnActionListener {
            override fun beforeActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {}

            override fun afterActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
                if (action.javaClass.simpleName != "RollbackAction") return

                // Note that checking changelist size immediately after rollback action will show changes as they were before the rollback
                // (even if the check is scheduled to be run later on EDT).
                // The following seems to be the only reliable way to do it.
                val afterUpdate = {
                    val changes = changeListManager.defaultChangeList.changes
                    onRollback(changes.isEmpty())
                }
                changeListManager.invokeAfterUpdate(afterUpdate, InvokeAfterUpdateMode.SILENT_CALLBACK_POOLED, null, ModalityState.any())
            }
        }

        busConnection = ApplicationManager.getApplication().messageBus.connect(project).also {
            it.subscribe(AnActionListener.TOPIC, listener)
        }
    }

    fun disable() {
        if (busConnection != null) {
            busConnection!!.disconnect()
            busConnection = null
        }
    }
}
