package limitedwip.common.vcs

import com.intellij.dvcs.DvcsUtil
import com.intellij.dvcs.push.PushSpec
import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ModalityState.nonModal
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.wm.IdeFocusManager

class CommitAndPushWithoutDialogAction: AnAction(AllIcons.Vcs.Push) {
    override fun actionPerformed(event: AnActionEvent) {
        commitWithoutDialogAndPush(event.project ?: return)
    }
}

fun commitWithoutDialogAndPush(project: Project) {
    invokeLater {
        doCommitWithoutDialog(project, onSuccess = {
            invokeLater {
                object : Backgroundable(project, "Pushing…", true, ALWAYS_BACKGROUND) {
                    override fun run(indicator: ProgressIndicator) = doPush(project)
                }.queue()
            }
        })
    }
}

fun invokeLater(modalityState: ModalityState = nonModal(), callback: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(callback, modalityState)
}

fun anActionEvent(vararg eventContext: Pair<String, Any>): AnActionEvent {
    class MapDataContext(val map: Map<String, Any>, val delegate: DataContext): DataContext {
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
        val pushSupport = DvcsUtil.getPushSupport(vcs)
        if (pushSupport != null) {
            VcsRepositoryManager.getInstance(project).repositories.forEach { repository ->
                val source = pushSupport.getSource(repository)
                val target = pushSupport.getDefaultTarget(repository)
                if (source != null && target != null) {
                    pushSupport.pusher.push(mapOf(repository to PushSpec(source, target)), null, true)
                }
            }
        }
    }
}
