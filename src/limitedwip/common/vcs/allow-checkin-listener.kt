package limitedwip.common.vcs

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsKey
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.checkin.BeforeCheckinDialogHandler
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory
import com.intellij.openapi.vcs.impl.CheckinHandlersManager
import com.intellij.util.containers.MultiMap
import limitedwip.common.pluginId
import java.util.Arrays.asList

private val log = Logger.getInstance(pluginId)

fun registerBeforeCheckInListener(listener: AllowCheckinListener) {
    // This is a hack caused by limitations of IntelliJ API.
    //  - cannot use CheckinHandlerFactory because:
    //		- CheckinHandler is used just before commit (and after displaying commit dialog)
    // 		- its CheckinHandlerFactory#createSystemReadyHandler() doesn't seem to be called
    //  - cannot use VcsCheckinHandlerFactory through extension points because need to register
    //    checkin handler for all VCSs available
    //  - cannot use CheckinHandlersManager#registerCheckinHandlerFactory() because it doesn't properly
    //    register VcsCheckinHandlerFactory
    //
    // Therefore, using reflection.

    accessField<MultiMap<VcsKey, VcsCheckinHandlerFactory>>(CheckinHandlersManager.getInstance(), asList("a", "b", "myVcsMap")) { multiMap ->
        for (key in multiMap.keySet()) {
            multiMap.putValue(key, DelegatingCheckinHandlerFactory(key as VcsKey, listener))
        }
    }
}

private inline fun <reified T> accessField(anObject: Any, possibleFieldNames: List<String>, f: (T) -> Unit) {
    for (field in anObject.javaClass.declaredFields) {
        if (possibleFieldNames.contains(field.name) && T::class.java.isAssignableFrom(field.type)) {
            field.isAccessible = true
            try {
                f.invoke(field.get(anObject) as T)
                return
            } catch (ignored: Exception) {
            }
        }
    }
    log.warn("Failed to access fields: $possibleFieldNames on '$anObject'")
}


interface AllowCheckinListener {
    fun allowCheckIn(project: Project, changes: List<Change>): Boolean
}


private class DelegatingCheckinHandlerFactory(key: VcsKey, private val listener: AllowCheckinListener): VcsCheckinHandlerFactory(key) {
    override fun createSystemReadyHandler(project: Project): BeforeCheckinDialogHandler? {
        return object: BeforeCheckinDialogHandler() {
            override fun beforeCommitDialogShown(project: Project, changes: List<Change>, executors: Iterable<CommitExecutor>, showVcsCommit: Boolean): Boolean {
                return listener.allowCheckIn(project, changes)
            }
        }
    }

    override fun createVcsHandler(panel: CheckinProjectPanel): CheckinHandler {
        return object: CheckinHandler() {}
    }
}
