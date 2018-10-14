// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.limbo.components

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import limitedwip.common.LimitedWipCheckin
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.limbo.Limbo
import limitedwip.autorevert.components.Ide as IdeFromAutoRevert

class LimboComponent(project: Project): AbstractProjectComponent(project) {
    private val unitTestsWatcher = UnitTestsWatcher(myProject)
    private lateinit var limbo: Limbo

    override fun projectOpened() {
        val ide = Ide(myProject)
        val settings = ServiceManager.getService(LimitedWipSettings::class.java)
        limbo = Limbo(ide, settings.toLimboSettings())
        ide.limbo = limbo

        unitTestsWatcher.start(object: UnitTestsWatcher.Listener {
            override fun onUnitTestSucceeded() = limbo.onUnitTestSucceeded()
            override fun onUnitTestFailed() = limbo.onUnitTestFailed()
        })

        LimitedWipCheckin.registerListener(myProject, object: LimitedWipCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) = limbo.onSuccessfulCommit()
        })

        LimitedWipConfigurable.registerSettingsListener(myProject, object: LimitedWipConfigurable.Listener {
            override fun onSettingsUpdate(settings: LimitedWipSettings) {
                limbo.onSettings(settings.toLimboSettings())
            }
        })
    }

    fun isCommitAllowed(): Boolean = limbo.isCommitAllowed()

    private fun LimitedWipSettings.toLimboSettings() = Limbo.Settings(enabled = this.limboEnabled)
}