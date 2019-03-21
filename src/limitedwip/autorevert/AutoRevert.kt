package limitedwip.autorevert


import limitedwip.autorevert.components.Ide

class AutoRevert(private val ide: Ide, private var settings: Settings) {
    companion object {
        private const val undefined = -1
    }
    private var isStarted = false
    private var startSeconds: Int = 0
    private var remainingSeconds: Int = 0
    private var postponeRevert: Boolean = false

    init {
        onSettingsUpdate(settings)
    }

    fun onTimer(seconds: Int, hasChanges: Boolean) {
        if (!settings.autoRevertEnabled) return
        if (!isStarted && hasChanges) start()
        if (!isStarted) return

        if (startSeconds == undefined || postponeRevert) {
            startSeconds = seconds - 1
        }
        val secondsPassed = seconds - startSeconds

        ide.showTimeTillRevert(remainingSeconds - secondsPassed + 1)

        if (secondsPassed >= remainingSeconds || postponeRevert) {
            startSeconds = undefined
            applyNewSettings()
            postponeRevert = ide.isCommitDialogOpen()
            if (!postponeRevert) {
                val revertedFilesCount = ide.revertCurrentChangeList()
                if (revertedFilesCount > 0 && settings.notifyOnRevert) {
                    ide.notifyThatChangesWereReverted()
                    // TODO stop()
                }
            }
        }
    }

    fun onAllChangesCommitted() {
        if (!isStarted) return

        startSeconds = undefined
        applyNewSettings()
        ide.showTimeTillRevert(remainingSeconds)
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

    private fun start() {
        isStarted = true
        startSeconds = undefined
        applyNewSettings()

        ide.showTimeTillRevert(remainingSeconds)
    }

    private fun stop() {
        isStarted = false
        ide.showThatAutoRevertStopped()
    }

    private fun applyNewSettings() {
        remainingSeconds = settings.secondsTillRevert
    }


    data class Settings(
        val autoRevertEnabled: Boolean,
        val secondsTillRevert: Int,
        val notifyOnRevert: Boolean,
        val showTimerInToolbar: Boolean = true
    )
}
