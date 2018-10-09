package limitedwip.autorevert.ui

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.Consumer
import limitedwip.autorevert.components.AutoRevertComponent
import limitedwip.common.PluginId
import java.awt.Component
import java.awt.event.MouseEvent

class AutoRevertStatusBarWidget : StatusBarWidget {

    private var text = ""

    override fun install(statusBar: StatusBar) {}

    override fun dispose() {}

    fun showTime(timeLeft: String) {
        text = timeTillRevertText + timeLeft
    }

    fun showStartedText() {
        text = startedText
    }

    fun showStoppedText() {
        text = stoppedText
    }

    override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? {
        return object : StatusBarWidget.TextPresentation {
            override fun getText() = this@AutoRevertStatusBarWidget.text

            @Suppress("OverridingDeprecatedMember") // Override to be compatible with older IJ versions.
            override fun getMaxPossibleText() = ""

            override fun getTooltipText() = "Click to start/stop auto-revert"

            override fun getClickConsumer(): Consumer<MouseEvent>? {
                return Consumer { mouseEvent ->
                    val dataContext = DataManager.getInstance().getDataContext(mouseEvent.component)
                    val project = PlatformDataKeys.PROJECT.getData(dataContext) ?: return@Consumer

                    val autoRevertComponent = project.getComponent(AutoRevertComponent::class.java) ?: return@Consumer

                    if (autoRevertComponent.isAutoRevertStarted) {
                        autoRevertComponent.stopAutoRevert()
                    } else {
                        autoRevertComponent.startAutoRevert()
                    }
                }
            }

            override fun getAlignment() = Component.CENTER_ALIGNMENT
        }
    }

    override fun ID() = PluginId.value + "_" + this.javaClass.simpleName

    companion object {
        private const val timeTillRevertText = "Auto-revert in "
        private const val startedText = "Auto-revert started"
        private const val stoppedText = "Auto-revert stopped"
    }
}
