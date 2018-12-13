// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.autorevert.components

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.InvokeAfterUpdateMode
import limitedwip.autorevert.AutoRevert
import limitedwip.autorevert.ui.AutoRevertStatusBarWidget
import limitedwip.common.TimerAppComponent
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.settings.toSeconds
import limitedwip.common.vcs.SuccessfulCheckin

class AutoRevertComponent(project: Project) : AbstractProjectComponent(project) {
    private val timer = TimerAppComponent.getInstance()
    private lateinit var autoRevert: AutoRevert

    val isAutoRevertStarted: Boolean
        get() = autoRevert.isStarted

    override fun projectOpened() {
        val settings = LimitedWipSettings.getInstance().toAutoRevertSettings()
        autoRevert = AutoRevert(Ide(myProject, settings, AutoRevertStatusBarWidget()), settings)

        timer.addListener(object : TimerAppComponent.Listener {
            override fun onUpdate(seconds: Int) {
                ApplicationManager.getApplication().invokeLater({ autoRevert.onTimer(seconds) }, ModalityState.any())
            }
        }, myProject)

        LimitedWipSettings.getInstance().addListener(myProject, object : LimitedWipSettings.Listener {
            override fun onUpdate(settings: LimitedWipSettings) = autoRevert.onSettingsUpdate(settings.toAutoRevertSettings())
        })

        SuccessfulCheckin.registerListener(myProject, object : SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allChangesAreCommitted: Boolean) {
                if (allChangesAreCommitted) autoRevert.onAllChangesCommitted()
            }
        })

        registerRollbackListener(myProject, onRollback = { allChangesRolledBack ->
            if (allChangesRolledBack) autoRevert.onAllChangesRolledBack()
        })
    }

    fun startAutoRevert() {
        autoRevert.start()
    }

    fun stopAutoRevert() {
        autoRevert.stop()
    }

    private fun LimitedWipSettings.toAutoRevertSettings() =
        AutoRevert.Settings(
            autoRevertEnabled,
            minutesTillRevert.toSeconds(),
            notifyOnRevert
        )
}

private fun registerRollbackListener(project: Project, onRollback: (allChangesRolledBack: Boolean) -> Unit) {
    val changeListManager = ChangeListManager.getInstance(project)
    ActionManager.getInstance().addAnActionListener(object : AnActionListener {
        override fun beforeActionPerformed(action: AnAction?, dataContext: DataContext?, event: AnActionEvent?) {}

        override fun afterActionPerformed(action: AnAction?, dataContext: DataContext?, event: AnActionEvent?) {
            if (action?.javaClass?.simpleName != "RollbackAction") return

            // Note that checking changelist size immediately after rollback action will show changes as they were before the rollback
            // (even if the check is scheduled to be run later on EDT).
            // The following seems the only reliable way to do it.
            val afterUpdate = {
                val changes = changeListManager.defaultChangeList.changes
                onRollback(changes.isEmpty())
            }
            changeListManager.invokeAfterUpdate(afterUpdate, InvokeAfterUpdateMode.SILENT_CALLBACK_POOLED, null, ModalityState.any())
        }
    }, project)
}
