package limitedwip.common.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.Disposer
import com.intellij.util.Range
import com.intellij.util.xmlb.XmlSerializerUtil
import limitedwip.common.pluginId
import limitedwip.common.settings.TcrAction.*

@State(
    name = "${pluginId}Settings",
    storages = arrayOf(Storage(file = "\$APP_CONFIG$/limitedwip.ui.settings.xml"))
)
data class LimitedWipSettings(
    var watchdogEnabled: Boolean = true,
    var maxLinesInChange: Int = 80,
    var notificationIntervalInMinutes: Int = 1,
    var noCommitsAboveThreshold: Boolean = true,
    var showRemainingChangesInToolbar: Boolean = true,

    var autoRevertEnabled: Boolean = false,
    var minutesTillRevert: Int = 2,
    var notifyOnRevert: Boolean = true,
    var showTimerInToolbar: Boolean = true,

    var tcrEnabled: Boolean = false,
    var notifyOnTcrRevert: Boolean = true,
    var tcrActionOnPassedTest: TcrAction = OpenCommitDialog
): PersistentStateComponent<LimitedWipSettings> {
    private val listeners = ArrayList<Listener>()

    override fun getState(): LimitedWipSettings? = this

    override fun loadState(state: LimitedWipSettings) {
        XmlSerializerUtil.copyBean(state, this)
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
        val minutesToRevertRange = Range(1, 99)
        val changedLinesRange = Range(1, 999)
        val notificationIntervalRange = Range(1, 99)

        fun getInstance(): LimitedWipSettings = ServiceManager.getService(LimitedWipSettings::class.java)
    }
}

fun Int.toSeconds(): Int = this * 60

enum class TcrAction {
    OpenCommitDialog,
    Commit,
    CommitAndPush
}