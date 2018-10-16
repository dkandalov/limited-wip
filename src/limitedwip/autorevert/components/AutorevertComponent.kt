// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.autorevert.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import limitedwip.autorevert.AutoRevert
import limitedwip.common.TimerAppComponent
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.vcs.SuccessfulCheckin

class AutoRevertComponent(project: Project) : AbstractProjectComponent(project) {
    private val timer = ApplicationManager.getApplication().getComponent(TimerAppComponent::class.java)
    private lateinit var autoRevert: AutoRevert

    val isAutoRevertStarted: Boolean
        get() = autoRevert.isStarted

    override fun projectOpened() {
        val settings = ServiceManager.getService(LimitedWipSettings::class.java).toAutoRevertSettings()
        autoRevert = AutoRevert(Ide(myProject, settings), settings)

        timer.addListener(object : TimerAppComponent.Listener {
            override fun onUpdate(seconds: Int) {
                ApplicationManager.getApplication().invokeLater({ autoRevert.onTimer(seconds) }, ModalityState.any())
            }
        }, myProject)

        LimitedWipConfigurable.registerSettingsListener(myProject, object : LimitedWipConfigurable.Listener {
            override fun onSettingsUpdate(settings: LimitedWipSettings) {
                autoRevert.onSettings(settings.toAutoRevertSettings())
            }
        })

        SuccessfulCheckin.registerListener(myProject, object : SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) {
                if (allFileAreCommitted) autoRevert.onAllFilesCommitted()
            }
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
            secondsTillRevert(),
            notifyOnRevert
        )
}
