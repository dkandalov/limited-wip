// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.limbo.components

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.vcs.SuccessfulCheckin
import limitedwip.limbo.Limbo
import limitedwip.autorevert.components.Ide as IdeFromAutoRevert

class LimboComponent(project: Project): AbstractProjectComponent(project) {
    private val unitTestsWatcher = UnitTestsWatcher(myProject)
    private lateinit var limbo: Limbo

    override fun projectOpened() {
        val ide = Ide(myProject)
        val settings = ServiceManager.getService(LimitedWipSettings::class.java)
        limbo = Limbo(ide, settings.toLimboSettings())
        ide.listener = object : Ide.Listener {
            override fun onForceCommit() = limbo.allowOneCommitWithoutChecks()
        }

        unitTestsWatcher.start(object: UnitTestsWatcher.Listener {
            override fun onUnitTestSucceeded() = limbo.onUnitTestSucceeded()
            override fun onUnitTestFailed() = limbo.onUnitTestFailed()
        })

        SuccessfulCheckin.registerListener(myProject, object: SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) = limbo.onSuccessfulCommit()
        })

        LimitedWipConfigurable.registerSettingsListener(myProject, object: LimitedWipConfigurable.Listener {
            override fun onSettingsUpdate(settings: LimitedWipSettings) {
                limbo.onSettings(settings.toLimboSettings())
            }
        })
    }

    fun isCommitAllowed(): Boolean = limbo.isCommitAllowed()

    private fun LimitedWipSettings.toLimboSettings() =
        Limbo.Settings(
            enabled = limboEnabled,
            notifyOnRevert = notifyOnLimboRevert,
            openCommitDialogOnPassedTest = openCommitDialogOnPassedTest
        )
}