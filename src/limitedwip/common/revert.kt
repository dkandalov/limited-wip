package limitedwip.common

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
import limitedwip.autorevert.components.Ide
import java.util.concurrent.atomic.AtomicInteger

private val logger = Logger.getInstance(Ide::class.java)

fun revertCurrentChangeList(project: Project): Int {
    val revertedFilesCount = AtomicInteger(0)
    ApplicationManager.getApplication().runWriteAction(Runnable {
        try {

            val changes = ChangeListManager.getInstance(project).defaultChangeList.changes
            revertedFilesCount.set(changes.size)
            if (changes.isEmpty()) return@Runnable

            RollbackWorker(project, "auto-revert", false).doRollback(changes, true, null, null)

            val changedFiles = changes.mapNotNull { change -> change.virtualFile }
            FileDocumentManager.getInstance().reloadFiles(*changedFiles.toTypedArray())

        } catch (e: Exception) {
            // observed exception while reloading project at the time of auto-revert
            logger.error("Error while doing revert", e)
        }
    })
    return revertedFilesCount.get()
}
