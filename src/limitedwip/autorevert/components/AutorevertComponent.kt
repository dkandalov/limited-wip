// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.autorevert.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import limitedwip.autorevert.AutoRevert
import limitedwip.autorevert.ui.AutoRevertStatusBarWidget
import limitedwip.common.TimerAppComponent
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.settings.toSeconds
import limitedwip.common.vcs.SuccessfulCheckin
import limitedwip.common.vcs.defaultChangeList

class AutoRevertComponent(project: Project) : AbstractProjectComponent(project) {
    private val timer = TimerAppComponent.getInstance()
    private lateinit var autoRevert: AutoRevert

    override fun projectOpened() {
        val settings = LimitedWipSettings.getInstance().toAutoRevertSettings()
        autoRevert = AutoRevert(Ide(myProject, settings, AutoRevertStatusBarWidget()), settings)

        timer.addListener(object : TimerAppComponent.Listener {
            override fun onUpdate(seconds: Int) {
                ApplicationManager.getApplication().invokeLater({
                    val hasChanges = myProject.defaultChangeList()?.changes?.isNotEmpty() ?: false
                    autoRevert.onTimer(seconds, hasChanges)
                }, ModalityState.any())
            }
        }, myProject)

        val rollbackListener = RollbackListener(myProject) { allChangesRolledBack ->
            if (allChangesRolledBack) autoRevert.onAllChangesRolledBack()
        }

        LimitedWipSettings.getInstance().addListener(myProject, object : LimitedWipSettings.Listener {
            override fun onUpdate(settings: LimitedWipSettings) {
                autoRevert.onSettingsUpdate(settings.toAutoRevertSettings())
                if (settings.autoRevertEnabled) rollbackListener.enable() else rollbackListener.disable()
            }
        })

        SuccessfulCheckin.registerListener(myProject, object : SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allChangesAreCommitted: Boolean) {
                if (allChangesAreCommitted) autoRevert.onAllChangesCommitted()
            }
        })
    }

    private fun LimitedWipSettings.toAutoRevertSettings() =
        AutoRevert.Settings(
            autoRevertEnabled,
            minutesTillRevert.toSeconds(),
            notifyOnRevert
        )
}
