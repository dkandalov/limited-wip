package limitedwip.watchdog.ui

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.Consumer
import limitedwip.watchdog.components.WatchdogComponent
import java.awt.Component
import java.awt.event.MouseEvent

class WatchdogStatusBarWidget : StatusBarWidget {

    private var text = ""

    override fun install(statusBar: StatusBar) {}

    override fun dispose() {}

    fun showChangeSize(linesInChange: String, maxLinesInChange: Int) {
        text = "$textPrefix$linesInChange/$maxLinesInChange"
    }

    fun showInitialText(maxLinesInChange: Int) {
        text = "$textPrefix-/$maxLinesInChange"
    }

    override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? {
        return object : StatusBarWidget.TextPresentation {
            override fun getText() = this@WatchdogStatusBarWidget.text

            @Suppress("OverridingDeprecatedMember") // Override to be compatible with older IJ versions.
            override fun getMaxPossibleText() = ""

            override fun getTooltipText() = "Shows amount of changed lines in current change list vs change size limit."

            override fun getClickConsumer(): Consumer<MouseEvent>? {
                return Consumer { mouseEvent ->
                    val dataContext = DataManager.getInstance().getDataContext(mouseEvent.component)
                    val project = PlatformDataKeys.PROJECT.getData(dataContext) ?: return@Consumer
                    val watchdogComponent = project.getComponent(WatchdogComponent::class.java) ?: return@Consumer

                    watchdogComponent.toggleSkipNotificationsUntilCommit()
                }
            }

            override fun getAlignment() = Component.CENTER_ALIGNMENT
        }
    }

    override fun ID() = "LimitedWIP_" + this.javaClass.simpleName

    companion object {
        private const val textPrefix = "Change size: "
    }
}
