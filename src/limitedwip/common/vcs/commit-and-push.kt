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
import com.intellij.openapi.vcs.VcsDataKeys.CHANGES
import com.intellij.openapi.vcs.actions.CommonCheckinProjectAction
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.wm.IdeFocusManager

fun openCommitDialog(changes: List<Change>? = null) {
    invokeLater {
        val actionEvent = if (changes == null) anActionEvent() else anActionEvent(CHANGES.name to changes.toTypedArray())
        CommonCheckinProjectAction().actionPerformed(actionEvent)
    }
}

fun commitWithoutDialogAndPush(project: Project) {
    invokeLater {
        doCommitWithoutDialog(project)
        invokeLater {
            doPush(project)
        }
    }
}

fun invokeLater(modalityState: ModalityState = NON_MODAL, callback: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(callback, modalityState)
}

private fun anActionEvent(vararg eventContext: Pair<String, Any>): AnActionEvent {
    class MapDataContext(val map: Map<String, Any>, val delegate: DataContext) : DataContext {
        override fun getData(dataId: String) = delegate.getData(dataId) ?: map[dataId]
    }
    val component = IdeFocusManager.findInstance().lastFocusedFrame!!.component
    val dataContext = DataManager.getInstance().getDataContext(component)
    val context = MapDataContext(eventContext.toMap(), dataContext)

    return AnActionEvent(
        null,
        context,
        ActionPlaces.UNKNOWN,
        Presentation(),
        ActionManager.getInstance(),
        0
    )
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
