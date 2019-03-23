package limitedwip.autorevert


import limitedwip.autorevert.components.Ide

class AutoRevert(private val ide: Ide, private var settings: Settings) {
    companion object {
        private const val undefined = -1
    }
    private var isStarted = false
    private var startSeconds: Int = 0
    private var remainingSeconds: Int = 0
    private var postponedRevert: Boolean = false

    init {
        onSettingsUpdate(settings)
    }

    fun onTimer(seconds: Int, hasChanges: Boolean) {
        if (!settings.autoRevertEnabled) return
        if (!isStarted && hasChanges) {
            isStarted = true
            startSeconds = seconds - 1
            applySecondsTillRevertSettings()
        }
        if (isStarted && !hasChanges) stop()
        if (!isStarted) return

        if (startSeconds == undefined) {
            startSeconds = seconds - 1
        }
        val secondsPassed = seconds - startSeconds

        ide.showTimeTillRevert(remainingSeconds - secondsPassed + 1)

        if (secondsPassed >= remainingSeconds || postponedRevert) {
            startSeconds = undefined
            postponedRevert = ide.isCommitDialogOpen()
            if (!postponedRevert) {
                val revertedFilesCount = ide.revertCurrentChangeList()
                if (revertedFilesCount > 0 && settings.notifyOnRevert) ide.notifyThatChangesWereReverted()
                stop()
            }
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

    private fun stop() {
        isStarted = false
        postponedRevert = false
        ide.showThatAutoRevertStopped()
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
