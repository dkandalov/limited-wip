package limitedwip.common.vcs

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.vcs.actions.CommonCheckinProjectAction
import com.intellij.openapi.wm.IdeFocusManager

fun openCommitDialog() {
    ApplicationManager.getApplication().invokeLater({
        val actionEvent = anActionEvent()
        CommonCheckinProjectAction().actionPerformed(actionEvent)
    }, ModalityState.NON_MODAL)
}

private fun anActionEvent(): AnActionEvent {
    val actionManager = ActionManager.getInstance()
    val dataContext = DataManager.getInstance().getDataContext(IdeFocusManager.findInstance().lastFocusedFrame!!.component)
    return AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, Presentation(), actionManager, 0)
}
