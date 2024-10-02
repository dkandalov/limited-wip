package limitedwip.common.vcs

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import limitedwip.common.pluginId

class SuccessfulCheckin : CheckinHandlerFactory() {

    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext) =
        object : CheckinHandler() {
            override fun checkinSuccessful() {
                val changeList = panel.project.defaultChangeList() ?: return
                val uncommittedFileCount = changeList.changes.size - panel.selectedChanges.size
                notifySettingsListeners(allChangesAreCommitted = uncommittedFileCount == 0)
            }
        }

    private fun notifySettingsListeners(allChangesAreCommitted: Boolean) {
        Extensions.getRootArea().getExtensionPoint<Listener>(extensionPointName).extensions.forEach { listener ->
            listener.onSuccessfulCheckin(allChangesAreCommitted)
        }
    }

    interface Listener {
        fun onSuccessfulCheckin(allChangesAreCommitted: Boolean)
    }

    companion object {
        private const val extensionPointName = "$pluginId.checkinListener"

        fun registerListener(disposable: Disposable, listener: Listener) {
            Extensions.getRootArea() // It has to be root area (not project area).
                .getExtensionPoint<Listener>(extensionPointName)
                .registerExtension(listener, disposable)
        }
    }
}
