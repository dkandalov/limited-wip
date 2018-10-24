package limitedwip.common.vcs

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import limitedwip.common.pluginId

class SuccessfulCheckin: CheckinHandlerFactory() {

    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object: CheckinHandler() {
            override fun checkinSuccessful() {
                val project = panel.project

                val changeListManager = ChangeListManager.getInstance(project)
                val uncommittedFileCount = changeListManager.defaultChangeList.changes.size - panel.selectedChanges.size
                val allFileAreCommitted = uncommittedFileCount == 0

                notifySettingsListeners(allFileAreCommitted)
            }
        }
    }

    private fun notifySettingsListeners(allFileAreCommitted: Boolean) {
        Extensions.getRootArea().getExtensionPoint<Listener>(extensionPointName).extensions.forEach { listener ->
            listener.onSuccessfulCheckin(allFileAreCommitted)
        }
    }

    interface Listener {
        fun onSuccessfulCheckin(allFileAreCommitted: Boolean)
    }

    companion object {
        private const val extensionPointName = "$pluginId.checkinListener"

        fun registerListener(disposable: Disposable, listener: Listener) {
            val extensionPoint = Extensions.getRootArea().getExtensionPoint<Listener>(extensionPointName)
            extensionPoint.registerExtension(listener)
            Disposer.register(disposable, Disposable { extensionPoint.unregisterExtension(listener) })
        }
    }
}
