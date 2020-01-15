package limitedwip.common.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.xmlb.XmlSerializerUtil
import limitedwip.common.pluginId
import limitedwip.common.settings.CommitMessageSource.LastCommit
import limitedwip.common.settings.LimitedWipSettings.Companion.never
import limitedwip.common.settings.TcrAction.Commit
import limitedwip.common.settings.TimeUnit.Minutes

@State(name = "${pluginId}Settings")
data class LimitedWipSettings(
    var watchdogEnabled: Boolean = true,
    var maxLinesInChange: Int = 80,
    var notificationIntervalInMinutes: Int = 1,
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
): PersistentStateComponent<LimitedWipSettings> {
    private val listeners = ArrayList<Listener>()

    override fun getState(): LimitedWipSettings? = this

    @Suppress("DEPRECATION")
    override fun loadState(state: LimitedWipSettings) {
        XmlSerializerUtil.copyBean(state, this)
        if (minutesTillRevert != never) {
            timeTillRevert = minutesTillRevert
            timeUnitTillRevert = Minutes
            minutesTillRevert = never
        }
        notifyListeners()
    }

    fun addListener(parentDisposable: Disposable, listener: Listener) {
        listeners.add(listener)
        Disposer.register(parentDisposable, Disposable { listeners.remove(listener) })
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

        fun getInstance(project: Project): LimitedWipSettings =
            ServiceManager.getService(project, LimitedWipSettings::class.java)
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