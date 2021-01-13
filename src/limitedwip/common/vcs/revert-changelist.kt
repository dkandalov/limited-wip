package limitedwip.common.vcs

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeList
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
import limitedwip.common.PathMatcher
import limitedwip.common.pluginId

private val logger = Logger.getInstance(pluginId)

fun revertCurrentChangeList(
    project: Project,
    doNotRevertTests: Boolean = false,
    doNotRevertFiles: Set<PathMatcher> = emptySet()
): Int {
    // Don't revert when there are no VCS registered.
    // (Note that it is possible to do a revert after removing VCS from project settings until IDE restart.)
    val changeList = project.defaultChangeList() ?: return 0
    invokeLater {
        doRevert(project, changeList.changes, doNotRevertTests, doNotRevertFiles)
    }
    return changeList.changes.size
}

private fun doRevert(
    project: Project,
    changes: Collection<Change>,
    doNotRevertTests: Boolean,
    doNotRevertFiles: Set<PathMatcher>
) {
    try {
        if (changes.isEmpty()) return

        // Reload files to prevent MemoryDiskConflictResolver showing with "File Cache Conflict" dialog.
        FileDocumentManager.getInstance().reloadFiles(*changes.mapNotNull { it.virtualFile }.toTypedArray())

        var changesToRevert = changes
        if (doNotRevertTests) {
            val modules = ModuleManager.getInstance(project).modules
            changesToRevert = changesToRevert.filter { change ->
                val file = change.virtualFile
                file == null || modules.none { it.moduleTestsWithDependentsScope.contains(file) }
            }
        }
        changesToRevert = changesToRevert.filter { change ->
            val file = change.virtualFile
            file == null || doNotRevertFiles.none { it.matches(file.path) }
        }

        val operationName = "$pluginId Revert"
        RollbackWorker(project, operationName, false).doRollback(changesToRevert, true, null, operationName)

    } catch (e: Exception) {
        // observed exception while reloading project at the time of auto-revert
        logger.error("Error while doing revert", e)
    }
}

fun Project.defaultChangeList(): ChangeList? =
    if (!ProjectLevelVcsManager.getInstance(this).hasActiveVcss()) null
    else ChangeListManager.getInstance(this).defaultChangeList
