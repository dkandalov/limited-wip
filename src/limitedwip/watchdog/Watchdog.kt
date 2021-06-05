package limitedwip.watchdog

import limitedwip.common.PathMatcher
import limitedwip.common.settings.LimitedWipSettings.Companion.never
import limitedwip.watchdog.components.WatchdogIde

class Watchdog(private val ide: WatchdogIde, private var settings: Settings) {
    private var secondsTillNotification = 0
    private var skipNotificationsUntilCommit = false
    private var allowOneCommitWithoutChecks = false

    init {
        onSettingsUpdate(settings)
    }

    fun onTimer() {
        if (!settings.enabled) return

        ide.calculateCurrentChangeListSizeInLines()

        val changeSize = ide.currentChangeListSizeInLines().applyExclusions()
        val exceededThreshold = changeSize.value > settings.maxLinesInChange

        secondsTillNotification--
        if (exceededThreshold && settings.notificationIntervalInSeconds != never && secondsTillNotification <= 0 && !skipNotificationsUntilCommit) {
            ide.showNotificationThatChangeSizeIsTooBig(changeSize, settings.maxLinesInChange)
            secondsTillNotification = settings.notificationIntervalInSeconds
        }
        if (!exceededThreshold) {
            ide.hideNotificationThatChangeSizeIsTooBig()
        }

        ide.showCurrentChangeListSize(changeSize, settings.maxLinesInChange)
    }

    fun onSettingsUpdate(settings: Settings) {
        ide.onSettingsUpdate(settings)
        secondsTillNotification = 0 // Set to 0 so that notifications are sent immediately after watchdog is started.
        if (this.settings.maxLinesInChange != settings.maxLinesInChange) {
            ide.showCurrentChangeListSize(ide.currentChangeListSizeInLines().applyExclusions(), settings.maxLinesInChange)
        }
        this.settings = settings
    }

    fun onSuccessfulCommit() {
        // This is a workaround to suppress notifications sent while commit dialog is open.
        ide.hideNotificationThatChangeSizeIsTooBig()

        skipNotificationsUntilCommit = false
        allowOneCommitWithoutChecks = false
    }

    fun isCommitAllowed(changeSizesWithPath: ChangeSizesWithPath): Boolean {
        if (allowOneCommitWithoutChecks || !settings.noCommitsAboveThreshold || !settings.enabled) return true
        if (changeSizesWithPath.applyExclusions().value > settings.maxLinesInChange) {
            ide.notifyThatCommitWasCancelled()
            return false
        }
        return true
    }

    fun toggleSkipNotificationsUntilCommit() {
        skipNotificationsUntilCommit(!skipNotificationsUntilCommit)
    }

    fun onSkipNotificationsUntilCommit() {
        skipNotificationsUntilCommit(true)
    }

    fun onForceCommit() {
        allowOneCommitWithoutChecks = true
        ide.commitWithoutDialog()
    }

    private fun ChangeSizesWithPath.applyExclusions(): ChangeSize {
        return ChangeSizesWithPath(
            value.filter { (path, _) -> settings.exclusions.none { it.matches(path) } }
        ).totalChangeSize
    }

    private fun skipNotificationsUntilCommit(value: Boolean) {
        skipNotificationsUntilCommit = value
        ide.showNotificationThatWatchdogIsDisableUntilNextCommit(value)
    }

    data class Settings(
        val enabled: Boolean,
        val maxLinesInChange: Int,
        val notificationIntervalInSeconds: Int,
        val showRemainingChangesInToolbar: Boolean,
        val noCommitsAboveThreshold: Boolean,
        val exclusions: Set<PathMatcher>
    )
}
