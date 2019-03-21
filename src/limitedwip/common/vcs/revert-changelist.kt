package limitedwip.common.vcs

import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeList
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
import limitedwip.common.pluginId

private val logger = Logger.getInstance(pluginId)

fun revertCurrentChangeList(project: Project): Int {
    // Don't revert when there are no VCS registered.
    // (Note that it is possible to do a revert after removing VCS from project settings until IDE restart.)
    val changeList = project.defaultChangeList() ?: return 0
    TransactionGuard.getInstance().submitTransactionLater(project, Runnable {
        doRevert(project, changeList.changes)
    })
    return changeList.changes.size
}

private fun doRevert(project: Project, changes: Collection<Change>) {
    try {
        if (changes.isNotEmpty()) {
            // Reload files to prevent MemoryDiskConflictResolver showing with "File Cache Conflict" dialog.
            FileDocumentManager.getInstance().reloadFiles(*changes.mapNotNull { it.virtualFile }.toTypedArray())

            val operationName = "$pluginId auto-revert"
            RollbackWorker(project, operationName, false).doRollback(changes, true, null, operationName)
        }
    } catch (e: Exception) {
        // observed exception while reloading project at the time of auto-revert
        logger.error("Error while doing revert", e)
    }
}

fun Project.defaultChangeList(): ChangeList? {
    if (!ProjectLevelVcsManager.getInstance(this).hasActiveVcss()) return null
    return ChangeListManager.getInstance(this).defaultChangeList
}
