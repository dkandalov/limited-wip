package limitedwip.autorevert


import limitedwip.autorevert.components.Ide

class AutoRevert(private val ide: Ide, private var settings: Settings) {
    private var isStarted = false
    private var remainingSeconds: Int = 0
    private var skippedRevert: Boolean = false
    private var paused: Boolean = false

    init {
        onSettingsUpdate(settings)
    }

    fun onTimer(hasChanges: Boolean) {
        if (!settings.enabled) return
        if (skippedRevert && revert()) return
        if (skippedRevert && remainingSeconds < 0) return
        if (isStarted && !hasChanges) return stop()
        if (!isStarted && hasChanges) start()
        if (!isStarted) return

        if (paused) {
            ide.showPaused()
        } else {
            remainingSeconds--
            ide.showTimeTillRevert(remainingSeconds + 1)
        }

        if (remainingSeconds < 0) {
            revert()
            stop()
        }
    }

    fun onPause() {
        if (!isStarted) return
        paused = true
        ide.showPaused()
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
        if (isStarted && !settings.enabled) {
            stop()
        }
        this.settings = settings
    }

    private fun start() {
        isStarted = true
        remainingSeconds = settings.secondsTillRevert
    }

    private fun stop() {
        isStarted = false
        paused = false
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


    data class Settings(
        val enabled: Boolean,
        val secondsTillRevert: Int,
        val notifyOnRevert: Boolean,
        val showTimerInToolbar: Boolean = true
    )
}
