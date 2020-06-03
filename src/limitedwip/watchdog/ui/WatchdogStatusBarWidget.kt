package limitedwip.watchdog.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.Consumer
import limitedwip.common.FloatingWidget
import limitedwip.common.pluginId
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent

class WatchdogStatusBarWidget(project: Project): StatusBarWidget {
    private val floatingWidget = FloatingWidget(
        projectComponent = WindowManager.getInstance().getIdeFrame(project)?.component,
        point = Point(
            Registry.intValue("limited-wip.watchdog.widget.x"),
            Registry.intValue("limited-wip.watchdog.widget.y")
        )
    )

    private val textPrefix = "Change size: "
    private var text = ""
    private var linesInChange = ""
    private var maxLinesInChange = ""
    var listener: Listener? = null

    fun showChangeSize(linesInChange: String, maxLinesInChange: Int) {
        this.linesInChange = linesInChange
        this.maxLinesInChange = maxLinesInChange.toString()
        text = "$textPrefix$linesInChange/$maxLinesInChange"
        floatingWidget.text = text
    }

    override fun install(statusBar: StatusBar) {}

    override fun dispose() {
        Disposer.dispose(floatingWidget)
    }

    override fun getPresentation() =
        object: StatusBarWidget.TextPresentation {
            override fun getText() = this@WatchdogStatusBarWidget.text
            override fun getTooltipText() = "Change size in lines: $linesInChange; threshold: $maxLinesInChange"
            override fun getClickConsumer() = Consumer<MouseEvent> { listener?.onClick() }
            override fun getAlignment() = Component.CENTER_ALIGNMENT
        }

    override fun ID() = pluginId + "_" + this.javaClass.simpleName

    fun addTo(statusBar: StatusBar) {
        statusBar.addWidget(this, "before Position")
        statusBar.updateWidget(ID())
        floatingWidget.show()
    }

    fun updateOn(statusBar: StatusBar) {
        statusBar.updateWidget(ID())
        floatingWidget.show()
    }

    fun removeFrom(statusBar: StatusBar) {
        statusBar.removeWidget(ID())
        floatingWidget.hide()
    }

    interface Listener {
        fun onClick()
    }
}
