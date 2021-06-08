package limitedwip.common.vcs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager
import com.intellij.vcs.commit.ChangeListCommitState
import com.intellij.vcs.commit.SingleChangeListCommitter
import com.intellij.vcs.commit.isAmendCommitMode
import com.intellij.vcs.log.VcsLogProvider
import limitedwip.common.settings.CommitMessageSource.ChangeListName
import limitedwip.common.settings.CommitMessageSource.LastCommit
import limitedwip.common.settings.LimitedWipSettings
import java.util.concurrent.CompletableFuture

class CommitWithoutDialogAction: AnAction(AllIcons.Actions.Commit) {
    override fun actionPerformed(event: AnActionEvent) {
        doCommitWithoutDialog(event.project ?: return)
    }
}

fun doCommitWithoutDialog(project: Project, isAmendCommit: Boolean = false, onSuccess: () -> Unit = {}) {
    // Don't attempt to commit if there are no VCS registered because it will throw an exception.
    val defaultChangeList = project.defaultChangeList() ?: return
    val changes = defaultChangeList.changes.toList()

    // This is not ideal because it only checks limited WIP commit handlers (and, for example, it won't optimise imports on commit)
    // but higher-level IntelliJ API for commits looks like a mess (e.g. you can instantiate ChangesViewCommitWorkflow but it
    // then fails because `commitState` is not initialised which required UI components).
    val canCommit = AllowCommit.listeners.all { listener -> listener.allowCommit(project, changes) }
    if (!canCommit) return

    val commitMessage = when (LimitedWipSettings.getInstance(project).commitMessageSource) {
        LastCommit     -> VcsConfiguration.getInstance(project).LAST_COMMIT_MESSAGE ?: ""
        ChangeListName -> defaultChangeList.name
    }

    // Need this starting from around IJ 2019.2 because otherwise changes are not included into commit.
    // This seems be related to change in VCS UI which has commit dialog built-in into the toolwindow.
    LineStatusTrackerManager.getInstanceImpl(project).resetExcludedFromCommitMarkers()

    // Save documents, otherwise commit doesn't work.
    FileDocumentManager.getInstance().saveAllDocuments()

    val committer = SingleChangeListCommitter(
        project,
        ChangeListCommitState(defaultChangeList as LocalChangeList, changes, commitMessage),
        createCommitContext(isAmendCommit),
        "Commit without dialog",
        isDefaultChangeListFullyIncluded = true
    )
    committer.addResultHandler { onSuccess() }
    committer.runCommit("Commit without dialog", sync = false)
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
