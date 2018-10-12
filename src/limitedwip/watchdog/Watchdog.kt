package limitedwip.watchdog

import limitedwip.watchdog.components.IdeAdapter

class Watchdog(private val ideAdapter: IdeAdapter, private var settings: Settings) {
    private var lastNotificationTime = undefined
    private var skipNotificationsUtilCommit = false

    init {
        onSettings(settings)
    }

    fun onTimer(seconds: Int) {
        if (!settings.enabled) return

        val changeSize = ideAdapter.currentChangeListSizeInLines()
        val exceededThreshold = changeSize.value > settings.maxLinesInChange
        val timeToNotify = lastNotificationTime == undefined || seconds - lastNotificationTime >= settings.notificationIntervalInSeconds

        if (timeToNotify && exceededThreshold && !skipNotificationsUtilCommit) {
            ideAdapter.onChangeSizeTooBig(changeSize, settings.maxLinesInChange)
            lastNotificationTime = seconds
        }
        if (!exceededThreshold) {
            ideAdapter.onChangeSizeWithinLimit()
        }

        ideAdapter.showCurrentChangeListSize(changeSize, settings.maxLinesInChange)
    }

    fun onSettings(settings: Settings) {
        ideAdapter.onSettingsUpdate(settings)
        lastNotificationTime = undefined
        this.settings = settings
    }

    fun onCommit() {
        // This is a workaround to suppress notifications sent while commit dialog is open.
        ideAdapter.onChangeSizeWithinLimit()

        skipNotificationsUtilCommit = false
    }

    fun skipNotificationsUntilCommit(value: Boolean) {
        skipNotificationsUtilCommit = value
        ideAdapter.onSkipNotificationUntilCommit(value)
    }

    fun toggleSkipNotificationsUntilCommit() {
        skipNotificationsUntilCommit(!skipNotificationsUtilCommit)
    }


    data class Settings(
        val enabled: Boolean,
        val maxLinesInChange: Int,
        val notificationIntervalInSeconds: Int,
        val showRemainingChangesInToolbar: Boolean
    )

    companion object {
        private const val undefined = -1
    }
}
