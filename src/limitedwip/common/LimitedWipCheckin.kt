package limitedwip.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory

class LimitedWipCheckin : CheckinHandlerFactory() {

    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object : CheckinHandler() {
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
        val extensionPoint = Extensions.getRootArea().getExtensionPoint<Listener>(EXTENSION_POINT_NAME)
        for (listener in extensionPoint.extensions) {
            listener.onSuccessfulCheckin(allFileAreCommitted)
        }
    }

    interface Listener {
        fun onSuccessfulCheckin(allFileAreCommitted: Boolean)
    }

    companion object {
        private val EXTENSION_POINT_NAME = "LimitedWIP.checkinListener"

        fun registerListener(disposable: Disposable, listener: Listener) {
            val extensionPoint = Extensions.getRootArea().getExtensionPoint<Listener>(EXTENSION_POINT_NAME)
            extensionPoint.registerExtension(listener)
            Disposer.register(disposable, Disposable { extensionPoint.unregisterExtension(listener) })
        }
    }
}
