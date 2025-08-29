package limitedwip.common.vcs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.*
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.*
import com.intellij.openapi.vcs.changes.*
import com.intellij.openapi.vcs.changes.shelf.ShelveSilentlyTitleProvider
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager
import com.intellij.vcs.commit.*
import com.intellij.vcs.log.VcsLogProvider
import limitedwip.common.settings.CommitMessageSource.*
import limitedwip.common.settings.LimitedWipSettings
import java.util.concurrent.CompletableFuture

class CommitWithoutDialogAction : AnAction(AllIcons.Actions.Commit) {
    override fun actionPerformed(event: AnActionEvent) {
        doCommitWithoutDialog(event.project ?: return)
    }
}

fun doCommitWithoutDialog(project: Project, isAmendCommit: Boolean = false, onSuccess: () -> Unit = {}) {
    // Don't attempt to commit if there are no VCS registered because it will throw an exception.
    val defaultChangeList = project.defaultChangeList() ?: return
    object : Backgroundable(project, "Committing…", true, ALWAYS_BACKGROUND) {
        override fun run(indicator: ProgressIndicator) {
            val changes = defaultChangeList.changes.toList()
            val titleProvider = ShelveSilentlyTitleProvider.EP_NAME
                .findFirstSafe { it.javaClass.simpleName == "LLMShelveSilentlyTitlesProvider" }

            // This is not ideal because it only checks limited WIP commit handlers (and, for example, it won't optimise imports on commit)
            // but higher-level IntelliJ API for commits looks like a mess (e.g. you can instantiate ChangesViewCommitWorkflow but it
            // then fails because `commitState` is not initialised which required UI components).
            val canCommit = AllowCommit.listeners.all { listener -> listener.allowCommit(project, changes) }
            if (!canCommit) return

            val commitMessage = when (LimitedWipSettings.getInstance(project).commitMessageSource) {
                LastCommit -> VcsConfiguration.getInstance(project).LAST_COMMIT_MESSAGE ?: ""
                ChangeListName -> defaultChangeList.name
                AIAssistant -> runBlockingCancellable {
                    titleProvider?.getTitle(project, changes) ?: ""
                }
            }

            // Need this starting from around IJ 2019.2 because otherwise changes are not included into commit.
            // This seems to be related to a change in VCS UI, which has a commit dialog built-in into the toolwindow.
            LineStatusTrackerManager.getInstanceImpl(project).resetExcludedFromCommitMarkers()

            invokeAndWaitIfNeeded {
                // Save documents, otherwise commit doesn't work.
                FileDocumentManager.getInstance().saveAllDocuments()
            }

            val committer = SingleChangeListCommitter.create(
                project,
                ChangeListCommitState(defaultChangeList as LocalChangeList, changes, commitMessage),
                createCommitContext(isAmendCommit),
                "Commit without dialog"
            )
            val resultHandler = ShowNotificationCommitResultHandler(committer)
            committer.addResultHandler(
                object : CommitterResultHandler {
                    override fun onSuccess() {
                        onSuccess()
                        resultHandler.onSuccess()
                    }
                }
            )
            committer.runCommit("Commit without dialog", sync = false)
        }
    }.queue()
}

private fun createCommitContext(isAmendCommit: Boolean): CommitContext =
    CommitContext().also {
        if (isAmendCommit) {
            it.isAmendCommitMode // Accessing field to force lazy-loading of IS_AMEND_COMMIT_MODE_KEY 🙄
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            // Search for Key by name because IS_AMEND_COMMIT_MODE_KEY is private.
            it.putUserData(Key.findKeyByName("Vcs.Commit.IsAmendCommitMode") as Key<Boolean>, true)
        }
    }

fun lastCommitExistOnlyOnCurrentBranch(project: Project): Boolean {
    val logProviders = VcsLogProvider.LOG_PROVIDER_EP.getExtensions(project)
    val roots = ProjectLevelVcsManager.getInstance(project).allVcsRoots
    roots.map { root ->
        val commitIsOnOtherBranches = CompletableFuture<Boolean>()
        ApplicationManager.getApplication().executeOnPooledThread {
            val logProvider = logProviders.find { it.supportedVcs == root.vcs?.keyInstanceMethod }!!
            val logData = logProvider.readFirstBlock(root.path) { 1 }
            val hash = logData.commits.last().id
            val branchesWithCommit = logProvider.getContainingBranches(root.path, hash)
            val currentBranch = logProvider.getCurrentBranch(root.path)
            commitIsOnOtherBranches.complete((branchesWithCommit - currentBranch).isNotEmpty())
        }
        if (commitIsOnOtherBranches.get()) return false
    }
    return true
}
