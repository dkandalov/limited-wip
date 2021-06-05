package limitedwip.tcr.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType.WARNING
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.actions.CommonCheckinProjectAction
import com.intellij.openapi.vcs.changes.Change
import limitedwip.common.PathMatcher
import limitedwip.common.pluginDisplayName
import limitedwip.common.vcs.*

class TcrIde(private val project: Project) {
    lateinit var listener: Listener

    init {
        AllowCommit.addListener(project, object: AllowCommit.Listener {
            override fun allowCommit(project: Project, changes: List<Change>) =
                project != this@TcrIde.project || listener.allowCommit()
        })
    }

    fun openCommitDialog() {
        invokeLater {
            CommonCheckinProjectAction().actionPerformed(anActionEvent())
        }
    }

    fun amendCommitWithoutDialog() {
        invokeLater {
            doCommitWithoutDialog(project, isAmendCommit = true)
        }
    }

    fun commitWithoutDialog() {
        invokeLater {
            doCommitWithoutDialog(project)
        }
    }

    fun commitWithoutDialogAndPush() {
        commitWithoutDialogAndPush(project)
    }

    fun revertCurrentChangeList(doNotRevertTests: Boolean, doNotRevertFiles: Set<PathMatcher>): Int {
        return revertCurrentChangeList(project, doNotRevertTests, doNotRevertFiles)
    }

    fun notifyThatCommitWasCancelled() {
        val notification = Notification(
            pluginDisplayName,
            "TCR - $pluginDisplayName",
            "Commit was cancelled because no tests were run. To force commit anyway <a href=\"\">click here</a>.",
            WARNING
        ).setListener { notification, _ ->
            notification.expire()
            listener.onForceCommit()
        }

        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    fun notifyThatChangesWereReverted() {
        val notification = Notification(
            pluginDisplayName,
            "TCR - $pluginDisplayName",
            "Current changelist was reverted",
            WARNING
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    fun defaultChangeListModificationCount(): Map<String, Long> {
        val changeList = project.defaultChangeList() ?: return emptyMap()
        return changeList.changes
            .mapNotNull { it.virtualFile }
            .associate { Pair(it.path, it.modificationCount) }
    }

    fun lastCommitExistOnlyOnCurrentBranch(): Boolean {
        return lastCommitExistOnlyOnCurrentBranch(project)
    }

    interface Listener {
        fun onForceCommit()
        fun allowCommit(): Boolean
    }
}