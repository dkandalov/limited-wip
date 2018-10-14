// Because ApplicationComponent and dataContextFromFocus were deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.limbo.components

import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import limitedwip.watchdog.components.VcsIdeUtil
import limitedwip.watchdog.components.VcsIdeUtil.registerBeforeCheckInListener

class DisableCommitWithoutRunningTestAppComponent: ApplicationComponent {

    override fun initComponent() {
        registerBeforeCheckInListener(object: VcsIdeUtil.CheckinListener {
            override fun allowCheckIn(project: Project, changes: List<Change>): Boolean {
                val limboComponent = project.getComponent(LimboComponent::class.java) ?: return true
                return limboComponent.isCommitAllowed()
            }
        })
    }

    override fun disposeComponent() {}

    override fun getComponentName() = this.javaClass.canonicalName!!
}
