package limitedwip.autorevert.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import limitedwip.autorevert.components.AutoRevertComponent

class StartOrStopAutoRevertAction : AnAction(AllIcons.Actions.Rollback) {
    private val startText = "Start auto-revert"
    private val stopText = "Stop auto-revert"

    override fun actionPerformed(event: AnActionEvent) {
        val autoRevertComponent = event.project?.getComponent(AutoRevertComponent::class.java) ?: return

        if (autoRevertComponent.isAutoRevertStarted) {
            autoRevertComponent.stopAutoRevert()
        } else {
            autoRevertComponent.startAutoRevert()
        }
    }

    override fun update(event: AnActionEvent) {
        val text = actionTextFor(event.project)
        event.presentation.text = text
        event.presentation.description = text
        event.presentation.isEnabled = event.project != null
    }

    private fun actionTextFor(project: Project?): String {
        val autoRevertComponent = project?.getComponent(AutoRevertComponent::class.java) ?: return startText
        return if (autoRevertComponent.isAutoRevertStarted) stopText else startText
    }
}
