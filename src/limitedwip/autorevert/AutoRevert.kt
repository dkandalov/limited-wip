package limitedwip.autorevert


import limitedwip.autorevert.components.IdeAdapter

class AutoRevert(private val ideNotifications: IdeAdapter) {

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

        ideNotifications.onAutoRevertStarted(remainingSeconds)
    }

    fun stop() {
        isStarted = false
        ideNotifications.onAutoRevertStopped()
    }

    fun onTimer(seconds: Int) {
        if (!isStarted) return

        if (startSeconds == -1) {
            startSeconds = seconds - 1
        }
        val secondsPassed = seconds - startSeconds

        ideNotifications.onTimeTillRevert(remainingSeconds - secondsPassed + 1)

        if (secondsPassed >= remainingSeconds) {
            startSeconds = -1
            applyNewSettings()
            val revertedFilesCount = ideNotifications.revertCurrentChangeList()
            if (revertedFilesCount > 0 && settings!!.notifyOnRevert) {
                ideNotifications.onChangesRevert()
            }
        }
    }

    fun onAllFilesCommitted() {
        if (!isStarted) return

        startSeconds = -1
        applyNewSettings()
        ideNotifications.onCommit(remainingSeconds)
    }

    fun onSettings(settings: Settings) {
        ideNotifications.onSettingsUpdate(settings)
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
    ) {
        constructor(secondsTillRevert: Int) : this(true, secondsTillRevert, true)
    }
}
