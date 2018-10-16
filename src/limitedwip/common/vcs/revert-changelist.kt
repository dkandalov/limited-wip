package limitedwip.common.vcs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
import limitedwip.common.pluginId
import java.util.concurrent.atomic.AtomicInteger

private val logger = Logger.getInstance(pluginId)

fun revertCurrentChangeList(project: Project): Int {
    val revertedFilesCount = AtomicInteger(0)
    val application = ApplicationManager.getApplication()
    application.invokeAndWait({
        application.runWriteAction {
            revertedFilesCount.set(doRevert(project))
        }
    }, ModalityState.NON_MODAL)
    return revertedFilesCount.get()
}

private fun doRevert(project: Project): Int {
    return try {
        val changes = ChangeListManager.getInstance(project).defaultChangeList.changes
        if (changes.isNotEmpty()) {
            RollbackWorker(project, "auto-revert", false).doRollback(changes, true, null, null)
        }
        changes.size
    } catch (e: Exception) {
        // observed exception while reloading project at the time of auto-revert
        logger.error("Error while doing revert", e)
        0
    }
}
