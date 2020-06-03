package limitedwip.autorevert.components

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import limitedwip.autorevert.AutoRevert
import limitedwip.autorevert.ui.AutoRevertStatusBarWidget
import limitedwip.common.TimerAppComponent
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.vcs.SuccessfulCheckin
import limitedwip.common.vcs.defaultChangeList
import limitedwip.common.vcs.invokeLater

class AutoRevertComponentStartup: StartupActivity {
    override fun runActivity(project: Project) = AutoRevertComponent(project).start()
}

class AutoRevertComponent(private val project: Project) {
    @Volatile private var enabled = false

    fun start() {
        val settings = LimitedWipSettings.getInstance(project).toAutoRevertSettings()
        val widget = AutoRevertStatusBarWidget(project)
        val autoRevert = AutoRevert(Ide(project, settings, widget), settings)
        widget.onClick {
            autoRevert.onPause()
        }

        enabled = settings.enabled

        TimerAppComponent.getInstance().addListener(project, object: TimerAppComponent.Listener {
            override fun onUpdate() {
                // Optimisation to avoid scheduling task when component is not enabled.
                if (!enabled) return

                invokeLater(ModalityState.any()) {
                    // Project can be closed (disposed) during handover between timer thread and EDT.
                    if (project.isDisposed) return@invokeLater
                    autoRevert.onTimer(hasChanges = project.defaultChangeList()?.changes?.isNotEmpty() ?: false)
                }
            }
        })

        val rollbackListener = RollbackListener(project) { allChangesRolledBack ->
            if (allChangesRolledBack) autoRevert.onAllChangesRolledBack()
        }

        LimitedWipSettings.getInstance(project).addListener(project, object: LimitedWipSettings.Listener {
            override fun onUpdate(settings: LimitedWipSettings) {
                autoRevert.onSettingsUpdate(settings.toAutoRevertSettings())
                enabled = settings.autoRevertEnabled
                if (enabled) rollbackListener.enable() else rollbackListener.disable()
            }
        })

        SuccessfulCheckin.registerListener(project, object: SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allChangesAreCommitted: Boolean) {
                if (allChangesAreCommitted) autoRevert.onAllChangesCommitted()
            }
        })
    }

    private fun LimitedWipSettings.toAutoRevertSettings() =
        AutoRevert.Settings(
            autoRevertEnabled,
            timeUnitTillRevert.toSeconds(timeTillRevert),
            notifyOnRevert
        )
}
