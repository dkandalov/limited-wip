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
    var listener: Listener? = null

    fun showInitialText(maxLinesInChange: Int) {
        text = "$textPrefix-/$maxLinesInChange"
    }

    fun showChangeSize(linesInChange: String, maxLinesInChange: Int) {
        text = "$textPrefix$linesInChange/$maxLinesInChange"
    }

    override fun install(statusBar: StatusBar) {}

    override fun dispose() {}

    override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? {
        return object: StatusBarWidget.TextPresentation {
            override fun getText() = this@WatchdogStatusBarWidget.text

            @Suppress("OverridingDeprecatedMember") // Override to be compatible with older IJ versions.
            override fun getMaxPossibleText() = ""

            override fun getTooltipText() = "Shows amount of changed lines in current change list vs change size limit."

            override fun getClickConsumer(): Consumer<MouseEvent>? {
                return Consumer { listener?.onClick() }
            }

            override fun getAlignment() = Component.CENTER_ALIGNMENT
        }
    }

    override fun ID() = pluginId + "_" + this.javaClass.simpleName

    interface Listener {
        fun onClick()
    }
}
