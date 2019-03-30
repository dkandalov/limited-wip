package limitedwip.autorevert


import limitedwip.autorevert.components.Ide

class AutoRevert(private val ide: Ide, private var settings: Settings) {
    private var isStarted = false
    private var startSeconds: Int = 0
    private var remainingSeconds: Int = 0
    private var skippedRevert: Boolean = false

    init {
        onSettingsUpdate(settings)
    }

    fun onTimer(seconds: Int, hasChanges: Boolean) {
        if (!settings.autoRevertEnabled) return
        if (skippedRevert && revert()) return
        if (skippedRevert && seconds - startSeconds > remainingSeconds) return
        if (isStarted && !hasChanges) return stop()
        if (!isStarted && hasChanges) start(seconds)
        if (!isStarted) return

        val secondsPassed = seconds - startSeconds
        ide.showTimeTillRevert(remainingSeconds - secondsPassed + 1)

        if (secondsPassed > remainingSeconds) {
            revert()
            stop()
        }
    }

    fun onAllChangesCommitted() {
        if (!isStarted) return
        stop()
    }

    fun onAllChangesRolledBack() {
        onAllChangesCommitted()
    }

    fun onSettingsUpdate(settings: Settings) {
        ide.onSettingsUpdate(settings)
        if (isStarted && !settings.autoRevertEnabled) {
            stop()
        }
        this.settings = settings
    }

    private fun start(seconds: Int) {
        isStarted = true
        startSeconds = seconds - 1
        applySecondsTillRevertSettings()
    }

    private fun stop() {
        isStarted = false
        ide.showThatAutoRevertStopped()
    }

    private fun revert(): Boolean {
        if (ide.isCommitDialogOpen()) {
            skippedRevert = true
            return false
        }
        val revertedFilesCount = ide.revertCurrentChangeList()
        if (revertedFilesCount > 0 && settings.notifyOnRevert) ide.notifyThatChangesWereReverted()
        skippedRevert = false
        return true
    }

    private fun applySecondsTillRevertSettings() {
        remainingSeconds = settings.secondsTillRevert
    }


    data class Settings(
        val autoRevertEnabled: Boolean,
        val secondsTillRevert: Int,
        val notifyOnRevert: Boolean,
        val showTimerInToolbar: Boolean = true
    )
}
