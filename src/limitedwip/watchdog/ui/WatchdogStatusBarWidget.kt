package limitedwip.watchdog.ui

import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.Consumer
import limitedwip.common.pluginId
import java.awt.Component
import java.awt.event.MouseEvent

class WatchdogStatusBarWidget: StatusBarWidget {
    private val textPrefix = "Change size: "
    private var text = ""
    private var linesInChange = ""
    private var maxLinesInChange = ""
    var listener: Listener? = null

    fun showChangeSize(linesInChange: String, maxLinesInChange: Int) {
        this.linesInChange = linesInChange
        this.maxLinesInChange = maxLinesInChange.toString()
        text = "$textPrefix$linesInChange/$maxLinesInChange"
    }

    override fun install(statusBar: StatusBar) {}

    override fun dispose() {}

    override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? {
        return object: StatusBarWidget.TextPresentation {
            override fun getText() = this@WatchdogStatusBarWidget.text
            override fun getTooltipText() = "Changed lines in current change list: $linesInChange; Change size limit: $maxLinesInChange"
            override fun getClickConsumer(): Consumer<MouseEvent>? = Consumer { listener?.onClick() }
            override fun getAlignment() = Component.CENTER_ALIGNMENT
        }
    }

    override fun ID() = pluginId + "_" + this.javaClass.simpleName

    interface Listener {
        fun onClick()
    }
}
