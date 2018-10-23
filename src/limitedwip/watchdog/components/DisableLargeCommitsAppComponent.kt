// Because ApplicationComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.watchdog.components

import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import limitedwip.common.vcs.AllowCheckinListener
import limitedwip.common.vcs.registerBeforeCheckInListener

class DisableLargeCommitsAppComponent : ApplicationComponent {

    override fun initComponent() {
        registerBeforeCheckInListener(object: AllowCheckinListener {
            override fun allowCheckIn(project: Project, changes: List<Change>): Boolean {
                val watchdogComponent = project.getComponent(WatchdogComponent::class.java) ?: return true
                return watchdogComponent.isCommitAllowed()
            }
        })
    }

    override fun disposeComponent() {}

    override fun getComponentName() = this.javaClass.canonicalName!!
}
