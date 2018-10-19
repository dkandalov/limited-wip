// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.limbo.components

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.vcs.SuccessfulCheckin
import limitedwip.limbo.Limbo
import limitedwip.autorevert.components.Ide as IdeFromAutoRevert

class LimboComponent(project: Project): AbstractProjectComponent(project) {
    private lateinit var limbo: Limbo

    override fun projectOpened() {
        val ide = Ide(myProject)
        val settings = ServiceManager.getService(LimitedWipSettings::class.java)
        limbo = Limbo(ide, settings.toLimboSettings())
        ide.listener = object : Ide.Listener {
            override fun onForceCommit() = limbo.forceOneCommit()
        }

//        ProjectLevelVcsManager.getInstance(myProject).
        VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileListener {
            override fun fileDeleted(event: VirtualFileEvent) = limbo.onFileChange()
            override fun fileMoved(event: VirtualFileMoveEvent) = limbo.onFileChange()
            override fun contentsChanged(event: VirtualFileEvent) = limbo.onFileChange()
            override fun fileCreated(event: VirtualFileEvent) = limbo.onFileChange()
            override fun fileCopied(event: VirtualFileCopyEvent) = limbo.onFileChange()
        }, myProject)

        UnitTestsWatcher(myProject).start(object: UnitTestsWatcher.Listener {
            override fun onUnitTestSucceeded() = limbo.onUnitTestSucceeded()
            override fun onUnitTestFailed() = limbo.onUnitTestFailed()
        })

        SuccessfulCheckin.registerListener(myProject, object: SuccessfulCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) = limbo.onSuccessfulCommit()
        })

        LimitedWipConfigurable.registerSettingsListener(myProject, object: LimitedWipConfigurable.Listener {
            override fun onSettingsUpdate(settings: LimitedWipSettings) {
                limbo.onSettings(settings.toLimboSettings())
            }
        })
    }

    fun isCommitAllowed(): Boolean = limbo.isCommitAllowed()

    private fun LimitedWipSettings.toLimboSettings() =
        Limbo.Settings(
            enabled = limboEnabled,
            notifyOnRevert = notifyOnLimboRevert,
            openCommitDialogOnPassedTest = openCommitDialogOnPassedTest
        )
}