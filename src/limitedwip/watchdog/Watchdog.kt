package limitedwip.watchdog

import limitedwip.watchdog.components.Ide

class Watchdog(private val ide: Ide, private var settings: Settings) {
    private var lastNotificationTime = undefined
    private var skipNotificationsUtilCommit = false
    private var allowOneCommitWithoutChecks = false

    init {
        onSettingsUpdate(settings)
    }

    fun onTimer(seconds: Int) {
        if (!settings.enabled) return

        ide.calculateCurrentChangeListSizeInLines()

        val changeSize = ide.currentChangeListSizeInLines()
        val exceededThreshold = changeSize.value > settings.maxLinesInChange
        val isTimeToNotify = lastNotificationTime == undefined || seconds - lastNotificationTime >= settings.notificationIntervalInSeconds

        if (exceededThreshold && isTimeToNotify && !skipNotificationsUtilCommit) {
            ide.showNotificationThatChangeSizeIsTooBig(changeSize, settings.maxLinesInChange)
            lastNotificationTime = seconds
        }
        if (!exceededThreshold) {
            ide.hideNotificationThatChangeSizeIsTooBig()
        }

        ide.showCurrentChangeListSize(changeSize, settings.maxLinesInChange)
    }

    fun onSettingsUpdate(settings: Settings) {
        ide.onSettingsUpdate(settings)
        lastNotificationTime = undefined
        if (this.settings.maxLinesInChange != settings.maxLinesInChange) {
            ide.showCurrentChangeListSize(ide.currentChangeListSizeInLines(), settings.maxLinesInChange)
        }
        this.settings = settings
    }

    fun onSuccessfulCommit() {
        // This is a workaround to suppress notifications sent while commit dialog is open.
        ide.hideNotificationThatChangeSizeIsTooBig()

        skipNotificationsUtilCommit = false
        allowOneCommitWithoutChecks = false
    }

    fun isCommitAllowed(changeSize: ChangeSize): Boolean {
        if (allowOneCommitWithoutChecks || !settings.noCommitsAboveThreshold) return true
        if (changeSize.value > settings.maxLinesInChange) {
            ide.notifyThatCommitWasCancelled()
            return false
        }
        return true
    }

    fun toggleSkipNotificationsUntilCommit() {
        skipNotificationsUntilCommit(!skipNotificationsUtilCommit)
    }

    fun onSkipNotificationsUntilCommit() {
        skipNotificationsUntilCommit(true)
    }

    fun onForceCommit() {
        allowOneCommitWithoutChecks = true
        ide.openCommitDialog()
    }

    private fun skipNotificationsUntilCommit(value: Boolean) {
        skipNotificationsUtilCommit = value
        ide.showNotificationThatWatchdogIsDisableUntilNextCommit(value)
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
