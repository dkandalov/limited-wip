package limitedwip.autorevert


import limitedwip.autorevert.components.Ide

class AutoRevert(private val ide: Ide, private var settings: Settings) {
    var isStarted = false
        private set
    private var startSeconds: Int = 0
    private var remainingSeconds: Int = 0

    init {
        onSettings(settings)
    }

    fun start() {
        if (!settings.autoRevertEnabled) return

        isStarted = true
        startSeconds = -1
        applyNewSettings()

        ide.showInUIThatAutoRevertStopped(remainingSeconds)
    }

    fun stop() {
        isStarted = false
        ide.showInUIThatAutoRevertStopped()
    }

    fun onTimer(seconds: Int) {
        if (!isStarted) return

        if (startSeconds == -1) {
            startSeconds = seconds - 1
        }
        val secondsPassed = seconds - startSeconds

        ide.showInUITimeTillRevert(remainingSeconds - secondsPassed + 1)

        if (secondsPassed >= remainingSeconds) {
            startSeconds = -1
            applyNewSettings()
            val revertedFilesCount = ide.revertCurrentChangeList()
            if (revertedFilesCount > 0 && settings.notifyOnRevert) {
                ide.showNotificationThatChangesWereReverted()
            }
        }
    }

    fun onAllFilesCommitted() {
        if (!isStarted) return

        startSeconds = -1
        applyNewSettings()
        ide.showInUITimeTillRevert(remainingSeconds)
    }

    fun onSettings(settings: Settings) {
        ide.onSettingsUpdate(settings)
        this.settings = settings
        if (isStarted && !settings.autoRevertEnabled) {
            stop()
        }
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
