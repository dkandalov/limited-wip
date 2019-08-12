package limitedwip.tcr.components

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import limitedwip.common.PathMatcher
import limitedwip.common.pluginDisplayName
import limitedwip.common.vcs.*


class Ide(private val project: Project) {
    lateinit var listener: Listener
    private var changesInLastCancelledCommit: List<Change>? = null

    init {
        AllowCommitAppComponent.getInstance().addListener(project, object: AllowCommitListener {
            override fun allowCommit(project: Project, changes: List<Change>): Boolean {
                val result = project != this@Ide.project || listener.allowCommit()
                changesInLastCancelledCommit = if (!result) changes else null
                return result
            }
        })
    }

    fun openCommitDialog() {
        openCommitDialog(changesInLastCancelledCommit)
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
            pluginDisplayName,
            "Commit was cancelled because no tests were run<br/> (<a href=\"\">Click here</a> to force commit anyway)",
            NotificationType.WARNING,
            NotificationListener { notification, _ ->
                notification.expire()
                listener.onForceCommit()
            }
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }

    fun notifyThatChangesWereReverted() {
        val notification = Notification(
            pluginDisplayName,
            pluginDisplayName,
            "Current changelist was reverted",
            NotificationType.WARNING
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