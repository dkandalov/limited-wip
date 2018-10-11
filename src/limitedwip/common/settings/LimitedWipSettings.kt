package limitedwip.common.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.Range
import com.intellij.util.xmlb.XmlSerializerUtil
import limitedwip.common.PluginId

@State(
    name = PluginId.value + "Settings",
    storages = arrayOf(Storage(file = "\$APP_CONFIG$/limitedwip.ui.settings.xml"))
)
class LimitedWipSettings: PersistentStateComponent<LimitedWipSettings> {

    var autoRevertEnabled = false
    var minutesTillRevert = 2
    var notifyOnRevert = true
    var showTimerInToolbar = true

    var watchdogEnabled = true
    var maxLinesInChange = 80
    var notificationIntervalInMinutes = 1
    var disableCommitsAboveThreshold = false
    var showRemainingChangesInToolbar = true

    fun secondsTillRevert(): Int = minutesTillRevert * 60

    fun notificationIntervalInSeconds(): Int = notificationIntervalInMinutes * 60

    override fun getState(): LimitedWipSettings? = this

    override fun loadState(state: LimitedWipSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val minutesToRevertRange = Range(1, 99)
        val changedLinesRange = Range(1, 999)
        val notificationIntervalRange = Range(1, 99)
    }
}
