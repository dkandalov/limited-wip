package limitedwip.autorevert

import limitedwip.autorevert.components.Ide

class AutoRevert(private val ide: Ide, private var settings: Settings) {
    private var started = false
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
        if (started && !hasChanges) return stop()
        if (!started && hasChanges) start()
        if (!started) return

        if (!paused) {
            remainingSeconds--
            ide.showTimeTillRevert(remainingSeconds + 1)
        }

        if (remainingSeconds < 0) {
            revert()
            stop()
        }
    }

    fun onPause() {
        if (!started) return
        paused = !paused
        if (paused) ide.showPaused()
    }

    fun onAllChangesCommitted() {
        if (!started) return
        stop()
    }

    fun onAllChangesRolledBack() {
        onAllChangesCommitted()
    }

    fun onSettingsUpdate(settings: Settings) {
        ide.onSettingsUpdate(settings)
        if (started && !settings.enabled) {
            stop()
        }
        this.settings = settings
    }

    private fun start() {
        started = true
        remainingSeconds = settings.secondsTillRevert
    }

    private fun stop() {
        started = false
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
