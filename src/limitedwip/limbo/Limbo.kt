package limitedwip.limbo

import limitedwip.limbo.components.Ide

class Limbo(private val ide: Ide, private var settings: Settings) {
    private var allowedToCommit = false
    private var allowOneCommitWithoutChecks = false
    private var testedModifications: ChangeListModifications? = null

    fun onUnitTestSucceeded(modifications: ChangeListModifications) {
        if (settings.disabled) return
        allowedToCommit = true
        testedModifications = modifications
        ide.openCommitDialog()
    }

    fun onUnitTestFailed() {
        if (settings.disabled) return
        ide.revertCurrentChangeList()
        if (settings.notifyOnRevert) ide.notifyThatChangesWereReverted()
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

    fun onSettings(settings: Settings) {
        this.settings = settings
    }

    data class Settings(
        val enabled: Boolean,
        val notifyOnRevert: Boolean,
        val openCommitDialogOnPassedTest: Boolean
    ) {
        val disabled = !enabled
    }

    data class ChangeListModifications(val value: Map<String, Long>)
}