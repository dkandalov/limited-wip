package limitedwip.autorevert.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.CommitResultHandler
import com.intellij.openapi.vcs.changes.InvokeAfterUpdateMode
import com.intellij.openapi.vcs.changes.actions.RefreshAction
import com.intellij.openapi.vcs.changes.ui.CommitHelper
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.impl.CheckinHandlersManager
import com.intellij.util.FunctionUtil
import java.util.*
import java.util.regex.Pattern


class QuickCommitAction : AnAction(AllIcons.Actions.Commit) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = AnAction.getEventProject(event) ?: return

        if (anySystemCheckinHandlerCancelsCommit(project)) return

        val runnable = Runnable {
            RefreshAction.doRefresh(project)

            val lastCommitMessage = VcsConfiguration.getInstance(project).LAST_COMMIT_MESSAGE
            val commitMessage = nextCommitMessage(lastCommitMessage)

            val defaultChangeList = ChangeListManager.getInstance(project).defaultChangeList
            if (defaultChangeList.changes.isEmpty()) {
                Messages.showInfoMessage(
                    project,
                    VcsBundle.message("commit.dialog.no.changes.detected.text"),
                    VcsBundle.message("commit.dialog.no.changes.detected.title")
                )
                return@Runnable
            }

            val noCheckinHandlers = ArrayList<CheckinHandler>()
            val commitHelper = CommitHelper(
                project,
                defaultChangeList,
                ArrayList(defaultChangeList.changes),
                "",
                commitMessage,
                noCheckinHandlers, true, true, FunctionUtil.nullConstant(),
                object : CommitResultHandler {
                    override fun onSuccess(commitMessage: String) {}
                    override fun onFailure() {}
                }
            )

            val committed = commitHelper.doCommit()
            if (committed) {
                VcsConfiguration.getInstance(project).saveCommitMessage(commitMessage)
            }
        }

        ChangeListManager.getInstance(project).invokeAfterUpdate(
            runnable,
            InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE,
            "Refreshing changelists...",
            ModalityState.current()
        )
    }

    companion object {

        private val messagePattern = Pattern.compile("^(.*?)(\\d+)$")

        /**
         * Couldn't find better way to "reuse" this code but to copy-paste it from
         * [com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog].
         */
        private fun anySystemCheckinHandlerCancelsCommit(project: Project): Boolean {
            val allActiveVcss = ProjectLevelVcsManager.getInstance(project).allActiveVcss
            val factoryList = CheckinHandlersManager.getInstance().getRegisteredCheckinHandlerFactories(allActiveVcss)
            return factoryList
                .map { it.createSystemReadyHandler(project) }
                .any { it != null && !it.beforeCommitDialogShown(project, ArrayList(), ArrayList(), false) }
        }

        fun nextCommitMessage(lastCommitMessage: String?): String {
            val message = lastCommitMessage ?: ""
            return if (message.endsWithDigits()) {
                val thisCommitNumber = Integer.valueOf(message.extractTailDigits()) + 1
                message.removeTailDigits() + thisCommitNumber
            } else {
                "$message 0"
            }
        }

        private fun String.endsWithDigits() = messagePattern.matcher(this).matches()

        private fun String.extractTailDigits(): String {
            val matcher = messagePattern.matcher(this)
            return if (matcher.matches()) matcher.group(2) else throw IllegalStateException()
        }

        private fun String.removeTailDigits(): String {
            val matcher = messagePattern.matcher(this)
            return if (matcher.matches()) matcher.group(1) else throw IllegalStateException()
        }
    }
}
