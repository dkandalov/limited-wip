package limitedwip.autorevert.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import limitedwip.autorevert.AutoRevert
import limitedwip.common.LimitedWipCheckin
import limitedwip.common.TimerComponent
import limitedwip.common.settings.LimitedWIPSettings
import limitedwip.common.settings.LimitedWipConfigurable

class AutoRevertComponent(project: Project) : AbstractProjectComponent(project) {
    private val timer = ApplicationManager.getApplication().getComponent(TimerComponent::class.java)
    private var autoRevert: AutoRevert? = null

    val isAutoRevertStarted: Boolean
        get() = autoRevert!!.isStarted

    override fun projectOpened() {
        val settings = ServiceManager.getService(LimitedWIPSettings::class.java)
        autoRevert = AutoRevert(IdeAdapter(myProject)).init(convert(settings))

        timer.addListener(TimerComponent.Listener { seconds ->
            ApplicationManager.getApplication().invokeLater({ autoRevert!!.onTimer(seconds) }, ModalityState.any())
        }, myProject)

        LimitedWipConfigurable.registerSettingsListener(myProject, LimitedWipConfigurable.Listener { settings ->
            autoRevert!!.onSettings(convert(settings))
        })

        LimitedWipCheckin.registerListener(myProject, LimitedWipCheckin.Listener { allFileAreCommitted ->
            if (allFileAreCommitted) autoRevert!!.onAllFilesCommitted()
        })
    }

    fun startAutoRevert() {
        autoRevert!!.start()
    }

    fun stopAutoRevert() {
        autoRevert!!.stop()
    }

    private fun convert(settings: LimitedWIPSettings): AutoRevert.Settings {
        return AutoRevert.Settings(
            settings.autoRevertEnabled,
            settings.secondsTillRevert(),
            settings.notifyOnRevert
        )
    }
}
