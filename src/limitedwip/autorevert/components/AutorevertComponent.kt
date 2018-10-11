// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.autorevert.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import limitedwip.autorevert.AutoRevert
import limitedwip.common.LimitedWipCheckin
import limitedwip.common.TimerComponent
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings

class AutoRevertComponent(project: Project) : AbstractProjectComponent(project) {
    private val timer = ApplicationManager.getApplication().getComponent(TimerComponent::class.java)
    private lateinit var autoRevert: AutoRevert

    val isAutoRevertStarted: Boolean
        get() = autoRevert.isStarted

    override fun projectOpened() {
        val settings = ServiceManager.getService(LimitedWipSettings::class.java)
        autoRevert = AutoRevert(IdeAdapter(myProject)).init(convert(settings))

        timer.addListener(object : TimerComponent.Listener {
            override fun onUpdate(seconds: Int) {
                ApplicationManager.getApplication().invokeLater({ autoRevert.onTimer(seconds) }, ModalityState.any())
            }
        }, myProject)

        LimitedWipConfigurable.registerSettingsListener(myProject, object : LimitedWipConfigurable.Listener {
            override fun onSettingsUpdate(settings: LimitedWipSettings) {
                autoRevert.onSettings(convert(settings))
            }
        })

        LimitedWipCheckin.registerListener(myProject, object : LimitedWipCheckin.Listener {
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

    private fun convert(settings: LimitedWipSettings): AutoRevert.Settings {
        return AutoRevert.Settings(
            settings.autoRevertEnabled,
            settings.secondsTillRevert(),
            settings.notifyOnRevert
        )
    }
}
