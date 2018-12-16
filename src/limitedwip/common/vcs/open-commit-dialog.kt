package limitedwip.common.vcs

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.actions.CommonCheckinProjectAction
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.wm.IdeFocusManager

fun openCommitDialog(changes: List<Change>? = null) {
    ApplicationManager.getApplication().invokeLater({
        val actionEvent = anActionEvent(changes)
        CommonCheckinProjectAction().actionPerformed(actionEvent)
    }, ModalityState.NON_MODAL)
}

private fun anActionEvent(changes: List<Change>?): AnActionEvent {
    val actionManager = ActionManager.getInstance()

    val component = IdeFocusManager.findInstance().lastFocusedFrame!!.component
    val dataContext = DataManager.getInstance().getDataContext(component)
    val map = if (changes != null) {
        mapOf(VcsDataKeys.CHANGES.name to changes.toTypedArray())
    } else {
        emptyMap()
    }

    return AnActionEvent(null, MapDataContext(map, dataContext), ActionPlaces.UNKNOWN, Presentation(), actionManager, 0)
}

private class MapDataContext(val map: Map<String, Any>, val delegate: DataContext): DataContext {
    override fun getData(dataId: String) = delegate.getData(dataId) ?: map[dataId]
}