package limitedwip.autorevert.ui

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

class AutoRevertStatusBarWidget(project: Project): StatusBarWidget {
    private val floatingWidget = FloatingWidget(
        projectComponent = WindowManager.getInstance().getIdeFrame(project)?.component,
        point = Point (
            Registry.intValue("limited-wip.autorevert.widget.x"),
            Registry.intValue("limited-wip.autorevert.widget.y")
        )
    )

    private var text = ""
    private var tooltipText = ""
    private lateinit var callback: () -> Unit

    fun onClick(callback: () -> Unit) {
        this.callback = callback
    }

    override fun install(statusBar: StatusBar) {}

    override fun dispose() {
        Disposer.dispose(floatingWidget)
    }

    fun showTimeLeft(timeLeft: String) {
        text = "Auto-revert in $timeLeft"
        tooltipText = "Auto-revert timer will be reset when all changes are committed or reverted"
        floatingWidget.text = text
    }

    fun showStartedText() {
        text = "Auto-revert started"
        tooltipText = "Auto-revert timer will be reset when all changes are committed or reverted"
        floatingWidget.text = text
    }

    fun showStoppedText() {
        text = "Auto-revert stopped"
        tooltipText = "Auto-revert timer will start as soon as you make some changes"
        floatingWidget.text = text
    }

    fun showPausedText() {
        text = "Auto-revert paused"
        tooltipText = "Auto-revert timer will continue next time you click on the widget"
        floatingWidget.text = text
    }

    override fun getPresentation() =
        object: StatusBarWidget.TextPresentation {
            override fun getText() = this@AutoRevertStatusBarWidget.text
            override fun getTooltipText() = this@AutoRevertStatusBarWidget.tooltipText
            override fun getClickConsumer(): Consumer<MouseEvent>? = Consumer { callback() }
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
}
