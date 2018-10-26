// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.tcr.components

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.vcs.AllowCommitAppComponent
import limitedwip.common.vcs.AllowCommitListener
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
        }

        UnitTestsWatcher(myProject).start(object: UnitTestsWatcher.Listener {
            override fun onUnitTestSucceeded() = tcr.onUnitTestSucceeded(ChangeListModifications(ide.defaultChangeListModificationCount()))
            override fun onUnitTestFailed() = tcr.onUnitTestFailed()
        })

        SuccessfulCheckin.registerListener(myProject, object: SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) = tcr.onSuccessfulCommit()
        })

        LimitedWipSettings.getInstance().addListener(myProject, object: LimitedWipSettings.Listener {
            override fun onUpdate(settings: LimitedWipSettings) = tcr.onSettingsUpdate(settings.toTcrSettings())
        })
        AllowCommitAppComponent.getInstance().addListener(myProject, object: AllowCommitListener {
            override fun allowCommit(project: Project, changes: List<Change>) =
                project != myProject || tcr.isCommitAllowed(ChangeListModifications(ide.defaultChangeListModificationCount()))
        })
    }

    private fun LimitedWipSettings.toTcrSettings() =
        Settings(
            enabled = tcrEnabled,
            notifyOnRevert = notifyOnTcrRevert,
            openCommitDialogOnPassedTest = openCommitDialogOnPassedTest
        )
}