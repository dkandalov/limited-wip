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
import limitedwip.limbo.Limbo.ChangeListModifications
import limitedwip.limbo.Limbo.Settings
import limitedwip.autorevert.components.Ide as IdeFromAutoRevert

class LimboComponent(project: Project): AbstractProjectComponent(project) {
    private lateinit var limbo: Limbo
    private lateinit var ide: Ide

    override fun projectOpened() {
        ide = Ide(myProject)
        val settings = ServiceManager.getService(LimitedWipSettings::class.java)
        limbo = Limbo(ide, settings.toLimboSettings())
        ide.listener = object : Ide.Listener {
            override fun onForceCommit() = limbo.forceOneCommit()
        }

        UnitTestsWatcher(myProject).start(object: UnitTestsWatcher.Listener {
            override fun onUnitTestSucceeded() = limbo.onUnitTestSucceeded(ChangeListModifications(ide.defaultChangeListModificationCount()))
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

    fun isCommitAllowed(): Boolean = limbo.isCommitAllowed(ChangeListModifications(ide.defaultChangeListModificationCount()))

    private fun LimitedWipSettings.toLimboSettings() =
        Settings(
            enabled = limboEnabled,
            notifyOnRevert = notifyOnLimboRevert,
            openCommitDialogOnPassedTest = openCommitDialogOnPassedTest
        )
}