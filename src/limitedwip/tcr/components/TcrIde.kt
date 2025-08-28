package limitedwip.tcr.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType.WARNING
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.actions.commit.CommonCheckinProjectAction
import limitedwip.common.PathMatcher
import limitedwip.common.pluginDisplayName
import limitedwip.common.vcs.*

class TcrIde(private val project: Project) {
    lateinit var listener: Listener

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

    fun revertCurrentChangeList(doNotRevertTests: Boolean, doNotRevertFiles: Set<PathMatcher>): Int =
        revertCurrentChangeList(project, doNotRevertTests, doNotRevertFiles)

    fun notifyThatCommitWasCancelled() {
        val notification = Notification(
            pluginDisplayName,
            "TCR - $pluginDisplayName",
            "Commit was cancelled. Please run tests to commit. To force commit anyway <a href=\"\">click here</a>.",
            WARNING
        ).setListener { notification, _ ->
            notification.expire()
            listener.onForceCommit()
        }

        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    fun notifyThatChangesWereReverted() {
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(
            Notification(
                pluginDisplayName,
                "TCR - $pluginDisplayName",
                "Current changelist was reverted",
                WARNING
            )
        )
    }

    fun defaultChangeListModificationCount(): Map<String, Long> {
        val changeList = project.defaultChangeList() ?: return emptyMap()
        return changeList.changes
            .mapNotNull { it.virtualFile }
            .associate { Pair(it.path, it.modificationCount) }
    }

    fun lastCommitExistOnlyOnCurrentBranch(): Boolean =
        lastCommitExistOnlyOnCurrentBranch(project)

    interface Listener {
        fun onForceCommit()
    }
}