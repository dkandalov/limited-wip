package limitedwip.common.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.xmlb.XmlSerializerUtil
import limitedwip.common.settings.CommitMessageSource.LastCommit
import limitedwip.common.settings.LimitedWipSettings.Companion.never
import limitedwip.common.settings.TcrAction.Commit
import limitedwip.common.settings.TimeUnit.Minutes

@State(name = "LimitedWIPPluginSettings", storages = [Storage(value = "limited-wip.xml")])
data class LimitedWipSettings(
    var watchdogEnabled: Boolean = true,
    var maxLinesInChange: Int = 80,
    var notificationIntervalInMinutes: Int = never,
    var noCommitsAboveThreshold: Boolean = false,
    var showRemainingChangesInToolbar: Boolean = true,
    var exclusions: String = "",

    var autoRevertEnabled: Boolean = false,
    var timeTillRevert: Int = 2,
    var timeUnitTillRevert: TimeUnit = Minutes,
    var notifyOnRevert: Boolean = true,
    var showTimerInToolbar: Boolean = true,

    var tcrEnabled: Boolean = false,
    var tcrActionOnPassedTest: TcrAction = Commit,
    var commitMessageSource: CommitMessageSource = LastCommit,
    var notifyOnTcrRevert: Boolean = false,
    var doNotRevertTests: Boolean = false,
    var doNotRevertFiles: String = "",

    var migratedFromOldLocation: Boolean = false
): PersistentStateComponent<LimitedWipSettings> {
    private val listeners = ArrayList<Listener>()

    override fun getState() = this

    @Suppress("DEPRECATION")
    override fun loadState(state: LimitedWipSettings) {
        XmlSerializerUtil.copyBean(state, this)
        notifyListeners()
    }

    fun addListener(parentDisposable: Disposable, listener: Listener) {
        listeners.add(listener)
        Disposer.register(parentDisposable) { listeners.remove(listener) }
    }

    fun notifyListeners() {
        listeners.forEach { it.onUpdate(this) }
    }

    interface Listener {
        fun onUpdate(settings: LimitedWipSettings)
    }

    companion object {
        const val never = Int.MAX_VALUE
        private val timeTillRevertRange = IntRange(1, 999)
        private val changedLinesRange = IntRange(1, 999)
        private val notificationIntervalRange = IntRange(1, never)

        fun isValidTimeTillRevert(duration: Int) = duration in timeTillRevertRange
        fun isValidChangedSizeRange(lineCount: Int) = lineCount in changedLinesRange
        fun isValidNotificationInterval(interval: Int) = interval in notificationIntervalRange

        fun getInstance(project: Project): LimitedWipSettings {
            val settings = ServiceManager.getService(project, LimitedWipSettings::class.java)
            return if (settings.migratedFromOldLocation) settings
            else settings.also {
                val oldSettings = OldLocation_LimitedWipSettings.getInstance(project)
                it.watchdogEnabled = oldSettings.watchdogEnabled
                it.maxLinesInChange = oldSettings.maxLinesInChange
                it.notificationIntervalInMinutes = oldSettings.notificationIntervalInMinutes
                it.noCommitsAboveThreshold = oldSettings.noCommitsAboveThreshold
                it.showRemainingChangesInToolbar = oldSettings.showRemainingChangesInToolbar
                it.exclusions = oldSettings.exclusions

                it.autoRevertEnabled = oldSettings.autoRevertEnabled
                it.timeTillRevert = oldSettings.timeTillRevert
                it.timeUnitTillRevert = oldSettings.timeUnitTillRevert
                it.notifyOnRevert = oldSettings.notifyOnRevert
                it.showTimerInToolbar = oldSettings.showTimerInToolbar

                it.tcrEnabled = oldSettings.tcrEnabled
                it.tcrActionOnPassedTest = oldSettings.tcrActionOnPassedTest
                it.commitMessageSource = oldSettings.commitMessageSource
                it.notifyOnTcrRevert = oldSettings.notifyOnTcrRevert
                it.doNotRevertTests = oldSettings.doNotRevertTests
                it.doNotRevertFiles = oldSettings.doNotRevertFiles

                it.migratedFromOldLocation = true
                // Reset to defaults so that settings are removed from the misc.xml
                oldSettings.resetToDefaults()
            }
        }
    }
}

@Suppress("ClassName")
@State(name = "LimitedWIPSettings") // Was stored in .idea/misc.xml
data class OldLocation_LimitedWipSettings(
    var watchdogEnabled: Boolean = true,
    var maxLinesInChange: Int = 80,
    var notificationIntervalInMinutes: Int = never,
    var noCommitsAboveThreshold: Boolean = false,
    var showRemainingChangesInToolbar: Boolean = true,
    var exclusions: String = "",

    var autoRevertEnabled: Boolean = false,
    @Deprecated("Should be removed in the future when it seems like all the users have upgraded")
    var minutesTillRevert: Int = never,
    var timeTillRevert: Int = 2,
    var timeUnitTillRevert: TimeUnit = Minutes,
    var notifyOnRevert: Boolean = true,
    var showTimerInToolbar: Boolean = true,

    var tcrEnabled: Boolean = false,
    var tcrActionOnPassedTest: TcrAction = Commit,
    var commitMessageSource: CommitMessageSource = LastCommit,
    var notifyOnTcrRevert: Boolean = false,
    var doNotRevertTests: Boolean = false,
    var doNotRevertFiles: String = ""
): PersistentStateComponent<OldLocation_LimitedWipSettings> {

    override fun getState() = this

    @Suppress("DEPRECATION")
    override fun loadState(state: OldLocation_LimitedWipSettings) {
        XmlSerializerUtil.copyBean(state, this)
        if (minutesTillRevert != never) {
            timeTillRevert = minutesTillRevert
            timeUnitTillRevert = Minutes
            minutesTillRevert = never
        }
    }

    fun resetToDefaults() {
        watchdogEnabled = true
        maxLinesInChange = 80
        notificationIntervalInMinutes = never
        noCommitsAboveThreshold = false
        showRemainingChangesInToolbar = true
        exclusions = ""

        autoRevertEnabled = false
        @Suppress("DEPRECATION")
        minutesTillRevert = never
        timeTillRevert = 2
        timeUnitTillRevert = Minutes
        notifyOnRevert = true
        showTimerInToolbar = true

        tcrEnabled = false
        tcrActionOnPassedTest = Commit
        commitMessageSource = LastCommit
        notifyOnTcrRevert = false
        doNotRevertTests = false
        doNotRevertFiles = ""
    }

    companion object {
        fun getInstance(project: Project): OldLocation_LimitedWipSettings =
            ServiceManager.getService(project, OldLocation_LimitedWipSettings::class.java)
    }
}

enum class TcrAction {
    OpenCommitDialog,
    Commit,
    AmendCommit,
    CommitAndPush
}

enum class CommitMessageSource {
    LastCommit,
    ChangeListName
}

enum class TimeUnit {
    Seconds,
    Minutes;

    fun toSeconds(duration: Int): Int =
        if (duration == never) never
        else when (this) {
            Seconds -> duration
            Minutes -> duration * 60
        }
}