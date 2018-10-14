package limitedwip.autorevert


import limitedwip.autorevert.components.Ide

class AutoRevert(private val ide: Ide) {

    private var settings: Settings? = null
    var isStarted = false
        private set
    private var startSeconds: Int = 0
    private var remainingSeconds: Int = 0

    fun init(settings: Settings): AutoRevert {
        onSettings(settings)
        return this
    }

    fun start() {
        if (!settings!!.autoRevertEnabled) return

        isStarted = true
        startSeconds = -1
        applyNewSettings()

        ide.onAutoRevertStarted(remainingSeconds)
    }

    fun stop() {
        isStarted = false
        ide.onAutoRevertStopped()
    }

    fun onTimer(seconds: Int) {
        if (!isStarted) return

        if (startSeconds == -1) {
            startSeconds = seconds - 1
        }
        val secondsPassed = seconds - startSeconds

        ide.onTimeTillRevert(remainingSeconds - secondsPassed + 1)

        if (secondsPassed >= remainingSeconds) {
            startSeconds = -1
            applyNewSettings()
            val revertedFilesCount = ide.revertCurrentChangeList()
            if (revertedFilesCount > 0 && settings!!.notifyOnRevert) {
                ide.showNotificationThatChangesWereReverted()
            }
        }
    }

    fun onAllFilesCommitted() {
        if (!isStarted) return

        startSeconds = -1
        applyNewSettings()
        ide.onCommit(remainingSeconds)
    }

    fun onSettings(settings: Settings) {
        ide.onSettingsUpdate(settings)
        this.settings = settings
        if (isStarted && !settings.autoRevertEnabled) {
            stop()
        }
    }

    private fun applyNewSettings() {
        if (remainingSeconds != settings!!.secondsTillRevert) {
            remainingSeconds = settings!!.secondsTillRevert
        }
    }


    data class Settings(
        val autoRevertEnabled: Boolean,
        val secondsTillRevert: Int,
        val notifyOnRevert: Boolean,
        val showTimerInToolbar: Boolean = true
    )
}
