package limitedwip.common.vcs

import com.intellij.dvcs.DvcsUtil
import com.intellij.dvcs.push.PushSpec
import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ModalityState.NON_MODAL
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.actions.CommonCheckinProjectAction
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.wm.IdeFocusManager

fun openCommitDialog(changes: List<Change>? = null) {
    invokeLater {
        val map = if (changes != null) mapOf(VcsDataKeys.CHANGES.name to changes.toTypedArray()) else emptyMap()
        val actionEvent = anActionEvent(map)
        CommonCheckinProjectAction().actionPerformed(actionEvent)
    }
}

fun doQuickCommit() {
    invokeLater {
        QuickCommitAction().actionPerformed(anActionEvent(emptyMap()))
    }
}

fun doQuickCommitAndPush(project: Project) {
    invokeLater {
        QuickCommitAction().actionPerformed(anActionEvent(emptyMap()))
        invokeLater {
            doPush(project)
        }
    }
}

private fun doPush(project: Project) {
    val allActiveVcss = ProjectLevelVcsManager.getInstance(project).allActiveVcss
    allActiveVcss.forEach { vcs ->
        val pushSupport = DvcsUtil.getPushSupport(vcs) ?: return
        VcsRepositoryManager.getInstance(project).repositories.forEach { repository ->
            val source = pushSupport.getSource(repository)
            val target = pushSupport.getDefaultTarget(repository) ?: return
            pushSupport.pusher.push(mapOf(repository to PushSpec(source, target)), null, true)
        }
    }
}

fun invokeLater(modalityState: ModalityState = NON_MODAL, callback: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(callback, modalityState)
}

private fun anActionEvent(map: Map<String, Any>): AnActionEvent {
    val component = IdeFocusManager.findInstance().lastFocusedFrame!!.component
    val dataContext = DataManager.getInstance().getDataContext(component)
    val context = MapDataContext(map, dataContext)

    return AnActionEvent(
        null,
        context,
        ActionPlaces.UNKNOWN,
        Presentation(),
        ActionManager.getInstance(),
        0
    )
}

private class MapDataContext(val map: Map<String, Any>, val delegate: DataContext) : DataContext {
    override fun getData(dataId: String) = delegate.getData(dataId) ?: map[dataId]
}