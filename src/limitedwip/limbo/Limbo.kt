package limitedwip.limbo

import limitedwip.limbo.components.Ide

class Limbo(private val ide: Ide, private var settings: Settings) {
    private var allowedToCommit = false
    private var allowOneCommitWithoutChecks = false

    fun onUnitTestSucceeded() {
        if (settings.disabled) return
        allowedToCommit = true
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

    fun isCommitAllowed(): Boolean {
        if (allowOneCommitWithoutChecks || settings.disabled) return true

        return if (!allowedToCommit) {
            ide.notifyThatCommitWasCancelled()
            false
        } else {
            true
        }
    }

    fun onSuccessfulCommit() {
        allowedToCommit = false
        allowOneCommitWithoutChecks = false
    }

    fun onSettings(settings: Settings) {
        this.settings = settings
    }

    fun onFileChange() {
        allowedToCommit = false
    }

    data class Settings(
        val enabled: Boolean,
        val notifyOnRevert: Boolean,
        val openCommitDialogOnPassedTest: Boolean
    ) {
        val disabled = !enabled
    }
}