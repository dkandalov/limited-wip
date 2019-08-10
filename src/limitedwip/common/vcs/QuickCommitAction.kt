package limitedwip.common.vcs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.changes.*
import com.intellij.openapi.vcs.changes.actions.RefreshAction
import com.intellij.openapi.vcs.changes.ui.CommitHelper
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.impl.CheckinHandlersManager
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager
import com.intellij.vcs.commit.isAmendCommitMode
import com.intellij.vcs.log.VcsLogProvider
import limitedwip.common.settings.CommitMessageSource.ChangeListName
import limitedwip.common.settings.CommitMessageSource.LastCommit
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.settings.TcrAction
import limitedwip.common.settings.TcrAction.*
import java.util.*
import java.util.concurrent.CompletableFuture


class QuickCommitAction: AnAction(AllIcons.Actions.Commit) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (anySystemCheckinHandlerCancelsCommit(project)) return
        // Don't attempt to commit if there are no VCS registered because it will throw an exception.
        val defaultChangeList = project.defaultChangeList() ?: return

        val runnable = Runnable {
            RefreshAction.doRefresh(project)

            // Need this starting from around IJ 2019.2 because otherwise changes are not included into commit.
            // This seems be related to change in VCS UI which has commit dialog built-in into the toolwindow.
            LineStatusTrackerManager.getInstanceImpl(project).resetExcludedFromCommitMarkers()

            if (defaultChangeList.changes.isEmpty()) {
                Messages.showInfoMessage(
                    project,
                    VcsBundle.message("commit.dialog.no.changes.detected.text"),
                    VcsBundle.message("commit.dialog.no.changes.detected.title")
                )
                return@Runnable
            }
            val commitMessage = when (LimitedWipSettings.getInstance(project).commitMessageSource) {
                LastCommit     -> VcsConfiguration.getInstance(project).LAST_COMMIT_MESSAGE ?: ""
                ChangeListName -> defaultChangeList.name
            }
            // Can't use empty message because CommitHelper will silently not commit changes.
            val nonEmptyCommitMessage = if (commitMessage.trim().isEmpty()) "empty message" else commitMessage

            // Commit "asynchronously" because right now in IJ this means doing it as background task
            // and this is better UX compared to a flashing modal commit progress window (which people have noticed and complained about).
            val commitSynchronously = false

            // Not getting rid of deprecated CommitHelper for now because the new API is too new
            // and moving to it will mean that the plugin will be only compatible with IJ 2019.
            val commitHelper = CommitHelper(
                project,
                defaultChangeList,
                defaultChangeList.changes.toList(),
                "",
                nonEmptyCommitMessage,
                emptyCheckinHandlers,
                true,
                commitSynchronously,
                createCommitContext(project),
                noopCommitHandler
            )
            commitHelper.doCommit()
        }

        ChangeListManager.getInstance(project).invokeAfterUpdate(
            runnable,
            InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE,
            "Refreshing changelists...",
            ModalityState.current()
        )
    }

    private fun createCommitContext(project: Project): PseudoMap<Any, Any> {
        return PseudoMap<Any, Any>().also {
            val isAmendCommit = LimitedWipSettings.getInstance(project).tcrActionOnPassedTest == AmendCommit
            if (isAmendCommit && lastCommitExistOnlyOnCurrentBranch(project)) {
                CommitContext().isAmendCommitMode // Accessing field to force lazy-loading of IS_AMEND_COMMIT_MODE_KEY 🙄
                @Suppress("UNCHECKED_CAST")
                // Search for Key by name because IS_AMEND_COMMIT_MODE_KEY is private.
                it.commitContext.putUserData(Key.findKeyByName("Vcs.Commit.IsAmendCommitMode") as Key<Boolean>, true)
            }
        }
    }

    private fun lastCommitExistOnlyOnCurrentBranch(project: Project): Boolean {
        val logProviders = VcsLogProvider.LOG_PROVIDER_EP.getExtensions(project)
        val roots = ProjectLevelVcsManager.getInstance(project).allVcsRoots
        roots.map { root ->
            val commitIsOnOtherBranches = CompletableFuture<Boolean>()
            ApplicationManager.getApplication().executeOnPooledThread {
                val logProvider = logProviders.find { it.supportedVcs == root.vcs?.keyInstanceMethod }!!
                val logData = logProvider.readFirstBlock(root.path!!) { 1 }
                val hash = logData.commits.last().id
                val branchesWithCommit = logProvider.getContainingBranches(root.path!!, hash)
                val currentBranch = logProvider.getCurrentBranch(root.path!!)
                commitIsOnOtherBranches.complete((branchesWithCommit - currentBranch).isNotEmpty())
            }
            if (commitIsOnOtherBranches.get()) return false
        }
        return true
    }

    companion object {
        private val emptyCheckinHandlers = emptyList<CheckinHandler>()
        private val noopCommitHandler = object: CommitResultHandler {
            override fun onSuccess(commitMessage: String) {}
            override fun onFailure() {}
        }

        /**
         * Couldn't find a better way to "reuse" this code but to copy-paste it from
         * [com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog].
         */
        private fun anySystemCheckinHandlerCancelsCommit(project: Project): Boolean {
            val allActiveVcss = ProjectLevelVcsManager.getInstance(project).allActiveVcss
            return CheckinHandlersManager.getInstance()
                .getRegisteredCheckinHandlerFactories(allActiveVcss)
                .map { it.createSystemReadyHandler(project) }
                .any { it != null && !it.beforeCommitDialogShown(project, ArrayList(), ArrayList(), false) }
        }
    }
}
