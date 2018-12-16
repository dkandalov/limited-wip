// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.tcr.components

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.vcs.SuccessfulCheckin
import limitedwip.tcr.Tcr
import limitedwip.tcr.Tcr.ChangeListModifications
import limitedwip.tcr.Tcr.Settings
import limitedwip.autorevert.components.Ide as IdeFromAutoRevert

class TcrComponent(project: Project): AbstractProjectComponent(project) {
    private lateinit var tcr: Tcr
    private lateinit var ide: Ide

    override fun projectOpened() {
        ide = Ide(myProject)
        tcr = Tcr(ide, LimitedWipSettings.getInstance().toTcrSettings())
        ide.listener = object: Ide.Listener {
            override fun onForceCommit() = tcr.forceOneCommit()
            override fun allowCommit() = tcr.isCommitAllowed(ChangeListModifications(ide.defaultChangeListModificationCount()))
        }

        UnitTestsWatcher(myProject).start(object: UnitTestsWatcher.Listener {
            override fun onUnitTestSucceeded() = tcr.onUnitTestSucceeded(ChangeListModifications(ide.defaultChangeListModificationCount()))
            override fun onUnitTestFailed() = tcr.onUnitTestFailed()
        })

        SuccessfulCheckin.registerListener(myProject, object: SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allChangesAreCommitted: Boolean) = tcr.onSuccessfulCommit()
        })

        LimitedWipSettings.getInstance().addListener(myProject, object: LimitedWipSettings.Listener {
            override fun onUpdate(settings: LimitedWipSettings) = tcr.onSettingsUpdate(settings.toTcrSettings())
        })
    }

    private fun LimitedWipSettings.toTcrSettings() =
        Settings(
            enabled = tcrEnabled,
            notifyOnRevert = notifyOnTcrRevert,
            openCommitDialogOnPassedTest = openCommitDialogOnPassedTest
        )
}