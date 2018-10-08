package limitedwip.autorevert.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.changes.*
import com.intellij.openapi.vcs.changes.actions.RefreshAction
import com.intellij.openapi.vcs.changes.ui.CommitHelper
import com.intellij.openapi.vcs.checkin.BaseCheckinHandlerFactory
import com.intellij.openapi.vcs.checkin.BeforeCheckinDialogHandler
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.impl.CheckinHandlersManager
import com.intellij.util.FunctionUtil

import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern


class QuickCommitAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = AnAction.getEventProject(event) ?: return

        if (anySystemCheckinHandlerCancelsCommit(project)) return

        val runnable = Runnable {
            RefreshAction.doRefresh(project)

            val lastCommitMessage = VcsConfiguration.getInstance(project).LAST_COMMIT_MESSAGE
            val commitMessage = nextCommitMessage(lastCommitMessage)

            val defaultChangeList = ChangeListManager.getInstance(project).defaultChangeList
            if (defaultChangeList.changes.isEmpty()) {
                Messages.showInfoMessage(project, VcsBundle.message("commit.dialog.no.changes.detected.text"),
                    VcsBundle.message("commit.dialog.no.changes.detected.title"))
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

        private val MESSAGE_PATTERN = Pattern.compile("^(.*?)(\\d+)$")

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
            var lastCommitMessage = lastCommitMessage
            if (lastCommitMessage == null) lastCommitMessage = ""

            return if (endsWithDigits(lastCommitMessage)) {
                val thisCommitNumber = Integer.valueOf(extractTailDigitsFrom(lastCommitMessage)) + 1
                removeTailDigitsFrom(lastCommitMessage) + thisCommitNumber
            } else {
                "$lastCommitMessage 0"
            }
        }

        private fun endsWithDigits(s: String) = MESSAGE_PATTERN.matcher(s).matches()

        private fun extractTailDigitsFrom(s: String): String {
            val matcher = MESSAGE_PATTERN.matcher(s)
            if (!matcher.matches()) throw IllegalStateException()
            return matcher.group(2)
        }

        private fun removeTailDigitsFrom(s: String): String {
            val matcher = MESSAGE_PATTERN.matcher(s)
            if (!matcher.matches()) throw IllegalStateException()
            return matcher.group(1)
        }
    }
}
