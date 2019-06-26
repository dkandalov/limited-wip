package limitedwip.autorevert.ui

import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.Consumer
import limitedwip.common.pluginId
import java.awt.Component
import java.awt.event.MouseEvent

class AutoRevertStatusBarWidget : StatusBarWidget {
    private var text = ""
    private var tooltipText = ""
    private lateinit var callback: () -> Unit

    fun onClick(callback: () -> Unit) {
        this.callback = callback
    }

    override fun install(statusBar: StatusBar) {}

    override fun dispose() {}

    fun showTimeLeft(timeLeft: String) {
        text = "Auto-revert in $timeLeft"
        tooltipText = "Auto-revert timer will be reset when all changes are committed or reverted"
    }

    fun showStartedText() {
        text = "Auto-revert started"
        tooltipText = "Auto-revert timer will be reset when all changes are committed or reverted"
    }

    fun showStoppedText() {
        text = "Auto-revert stopped"
        tooltipText = "Auto-revert timer will start as soon as you make some changes"
    }

    override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? {
        return object : StatusBarWidget.TextPresentation {
            override fun getText() = this@AutoRevertStatusBarWidget.text

            override fun getTooltipText() = this@AutoRevertStatusBarWidget.tooltipText

            override fun getClickConsumer(): Consumer<MouseEvent>? = Consumer { callback() }

            override fun getAlignment() = Component.CENTER_ALIGNMENT

            @Suppress("OverridingDeprecatedMember") // Override to be compatible with older IJ versions.
            override fun getMaxPossibleText() = ""
        }
    }

    override fun ID() = pluginId + "_" + this.javaClass.simpleName
}
