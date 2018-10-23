package limitedwip.watchdog

import limitedwip.watchdog.components.Ide

class Watchdog(private val ide: Ide, private var settings: Settings) {
    private var lastNotificationTime = undefined
    private var skipNotificationsUtilCommit = false

    init {
        onSettings(settings)
    }

    fun onTimer(seconds: Int) {
        if (!settings.enabled) return

        val changeSize = ide.currentChangeListSizeInLines()
        val exceededThreshold = changeSize.value > settings.maxLinesInChange
        val timeToNotify = lastNotificationTime == undefined || seconds - lastNotificationTime >= settings.notificationIntervalInSeconds

        if (timeToNotify && exceededThreshold && !skipNotificationsUtilCommit) {
            ide.showNotificationThatChangeSizeIsTooBig(changeSize, settings.maxLinesInChange)
            lastNotificationTime = seconds
        }
        if (!exceededThreshold) {
            ide.hideNotificationThatChangeSizeIsTooBig()
        }

        ide.showCurrentChangeListSize(changeSize, settings.maxLinesInChange)
    }

    fun onSettings(settings: Settings) {
        ide.onSettingsUpdate(settings)
        lastNotificationTime = undefined
        this.settings = settings
    }

    fun onCommit() {
        // This is a workaround to suppress notifications sent while commit dialog is open.
        ide.hideNotificationThatChangeSizeIsTooBig()

        skipNotificationsUtilCommit = false
    }

    fun skipNotificationsUntilCommit(value: Boolean) {
        skipNotificationsUtilCommit = value
        ide.showNotificationThatWatchdogIsDisableUntilNextCommit(value)
    }

    fun toggleSkipNotificationsUntilCommit() {
        skipNotificationsUntilCommit(!skipNotificationsUtilCommit)
    }


    data class Settings(
        val enabled: Boolean,
        val maxLinesInChange: Int,
        val notificationIntervalInSeconds: Int,
        val showRemainingChangesInToolbar: Boolean,
        val noCommitsAboveThreshold: Boolean
    )

    companion object {
        private const val undefined = -1
    }
}
