package limitedwip.tcr

import limitedwip.common.PathMatcher
import limitedwip.common.settings.TcrAction
import limitedwip.common.settings.TcrAction.*
import limitedwip.tcr.components.Ide

class Tcr(private val ide: Ide, private var settings: Settings) {
    private var allowedToCommit = false
    private var allowOneCommitWithoutChecks = false
    private var testedModifications: ChangeListModifications? = null

    fun onUnitTestSucceeded(modifications: ChangeListModifications) {
        if (settings.disabled) return
        allowedToCommit = true
        testedModifications = modifications
        if (modifications.value.isNotEmpty()) {
            when (settings.actionOnPassedTest) {
                OpenCommitDialog -> ide.openCommitDialog()
                AmendCommit      -> ide.commitWithoutDialog()
                Commit           -> ide.commitWithoutDialog()
                CommitAndPush    -> ide.commitWithoutDialogAndPush()
            }
        }
    }

    fun onUnitTestFailed() {
        if (settings.disabled) return
        val revertedFileCount = ide.revertCurrentChangeList(settings.doNotRevertTests, settings.doNotRevertFiles)
        if (revertedFileCount > 0 && settings.notifyOnRevert) ide.notifyThatChangesWereReverted()
        allowedToCommit = false
    }

    fun forceOneCommit() {
        allowOneCommitWithoutChecks = true
        ide.openCommitDialog()
    }

    fun isCommitAllowed(modifications: ChangeListModifications): Boolean {
        if (allowOneCommitWithoutChecks || settings.disabled) return true
        if (testedModifications != modifications) allowedToCommit = false
        if (!allowedToCommit) ide.notifyThatCommitWasCancelled()
        return allowedToCommit
    }

    fun onSuccessfulCommit() {
        allowedToCommit = false
        allowOneCommitWithoutChecks = false
    }

    fun onSettingsUpdate(settings: Settings) {
        this.settings = settings
    }

    data class Settings(
        val enabled: Boolean,
        val notifyOnRevert: Boolean,
        val actionOnPassedTest: TcrAction,
        val doNotRevertTests: Boolean,
        val doNotRevertFiles: Set<PathMatcher>
    ) {
        val disabled = !enabled
    }

    data class ChangeListModifications(val value: Map<String, Long>)
}