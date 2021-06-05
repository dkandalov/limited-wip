package limitedwip.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.Gray
import com.intellij.ui.LightColors
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.GridBagLayout
import java.awt.Point
import javax.swing.JComponent

class FloatingWidget(private val point: Point, private val projectComponent: JComponent?): Disposable {
    private val label = JBLabel("<html></html>")
    private val panel = JBPanel<JBPanel<*>>().also {
        it.layout = GridBagLayout()
        it.background = LightColors.SLIGHTLY_GRAY
        val scale = 20
        it.preferredSize = Dimension(16 * scale, 3 * scale)
        it.add(label)
    }
    private var balloon: Balloon? = null

    var text: String
        get() = label.text
        set(value) {
            label.text = value
        }

    fun show() {
        if (Registry.`is`("limited-wip.floating-widgets.enabled", false)) {
            dispose()
            balloon = createBalloon().also {
                val relativePoint =
                    if (projectComponent == null) RelativePoint(point)
                    else RelativePoint(projectComponent, point)
                it.show(relativePoint, Balloon.Position.below)
            }
        }
    }

    fun hide() {
        balloon?.hide()
    }

    override fun dispose() {
        balloon.ifNotNull { Disposer.dispose(it) }
    }

    private fun createBalloon(): Balloon =
        JBPopupFactory.getInstance().createBalloonBuilder(panel)
            .setFadeoutTime(0)
            .setFillColor(Gray.TRANSPARENT)
            .setBorderInsets(JBUI.emptyInsets())
            .setAnimationCycle(0)
            .setCloseButtonEnabled(false)
            .setShowCallout(false)
            .setHideOnClickOutside(false)
            .setHideOnKeyOutside(false)
            .setHideOnFrameResize(false)
            .setHideOnAction(false)
            .setBlockClicksThroughBalloon(true)
            .createBalloon()
}