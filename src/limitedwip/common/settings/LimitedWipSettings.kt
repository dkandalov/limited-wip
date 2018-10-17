package limitedwip.common.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.Range
import com.intellij.util.xmlb.XmlSerializerUtil
import limitedwip.common.pluginId

@State(
    name = "${pluginId}Settings",
    storages = arrayOf(Storage(file = "\$APP_CONFIG$/limitedwip.ui.settings.xml"))
)
data class LimitedWipSettings(
    var autoRevertEnabled: Boolean = false,
    var minutesTillRevert: Int = 2,
    var notifyOnRevert: Boolean = true,
    var showTimerInToolbar: Boolean = true,

    var watchdogEnabled: Boolean = true,
    var maxLinesInChange: Int = 80,
    var notificationIntervalInMinutes: Int = 1,
    var disableCommitsAboveThreshold: Boolean = false,
    var showRemainingChangesInToolbar: Boolean = true,

    var limboEnabled: Boolean = false,
    var notifyOnLimboRevert: Boolean = true,
    var openCommitDialogOnPassedTest: Boolean = true
): PersistentStateComponent<LimitedWipSettings> {

    fun secondsTillRevert(): Int = minutesTillRevert * 60
    fun notificationIntervalInSeconds(): Int = notificationIntervalInMinutes * 60

    override fun getState(): LimitedWipSettings? = this

    override fun loadState(state: LimitedWipSettings) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        val minutesToRevertRange = Range(1, 99)
        val changedLinesRange = Range(1, 999)
        val notificationIntervalRange = Range(1, 99)
    }
}
