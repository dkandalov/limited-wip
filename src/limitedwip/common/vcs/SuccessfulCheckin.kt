package limitedwip.common.vcs

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import limitedwip.common.pluginId
import limitedwip.common.vcs.SuccessfulCheckin.Listener

class SuccessfulCheckin : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext) =
        object : CheckinHandler() {
            override fun checkinSuccessful() {
                val changeList = panel.project.defaultChangeList() ?: return
                val uncommittedFileCount = changeList.changes.size - panel.selectedChanges.size
                notifySettingsListeners(allChangesAreCommitted = uncommittedFileCount == 0)
            }
        }

    interface Listener {
        fun onSuccessfulCheckin(allChangesAreCommitted: Boolean)
    }
}

private const val extensionPointName = "$pluginId.checkinListener"

@Suppress("UnstableApiUsage")
private fun notifySettingsListeners(allChangesAreCommitted: Boolean) {
    ApplicationManager.getApplication().extensionArea
        .getExtensionPoint<Listener>(extensionPointName).extensions
        .forEach { listener ->
            listener.onSuccessfulCheckin(allChangesAreCommitted)
        }
}

@Suppress("UnstableApiUsage")
fun registerSuccessfulCheckinListener(disposable: Disposable, listener: Listener) {
    ApplicationManager.getApplication().extensionArea // It has to be root area (not project area).
        .getExtensionPoint<Listener>(extensionPointName)
        .registerExtension(listener, disposable)
}

