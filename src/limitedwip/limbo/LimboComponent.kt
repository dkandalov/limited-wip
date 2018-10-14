// Because AbstractProjectComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")
package limitedwip.limbo

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import limitedwip.autorevert.components.Ide as IdeFromAutoRevert
import limitedwip.common.LimitedWipCheckin
import limitedwip.common.pluginDisplayName
import limitedwip.common.settings.LimitedWipConfigurable
import limitedwip.common.settings.LimitedWipSettings
import limitedwip.limbo.Limbo.Amount.Companion.zero

class Ide(
    private val project: Project,
    private val ideFromAutoRevert: IdeFromAutoRevert
) {
    lateinit var limbo: Limbo

    fun revertCurrentChangeList() {
        ideFromAutoRevert.revertCurrentChangeList()
    }

    fun notifyThatCommitWasCancelled() {
        val notification = Notification(
            pluginDisplayName,
            pluginDisplayName,
            "Commit was cancelled because no tests were run<br/> (<a href=\"\">Click here</a> to force commit anyway)",
            NotificationType.ERROR,
            NotificationListener { _, _ ->
                limbo.allowOneCommitWithoutChecks()
            }
        )
        project.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }
}

class Limbo(private val ide: Ide, private var settings: Settings) {
    private var amountOfTestsRun = zero
    private var allowOneCommitWithoutChecks = false

    init {
        onSettings(settings)
    }

    fun onUnitTestSucceeded() {
        amountOfTestsRun = amountOfTestsRun++
    }

    fun onUnitTestFailed() {
        ide.revertCurrentChangeList()
        amountOfTestsRun = zero
    }

    fun allowOneCommitWithoutChecks() {
        allowOneCommitWithoutChecks = true
    }

    fun isCommitAllowed(): Boolean {
        if (allowOneCommitWithoutChecks) {
            allowOneCommitWithoutChecks = false
            return true
        }
        if (!settings.enabled) return true

        return if (amountOfTestsRun == zero) {
            ide.notifyThatCommitWasCancelled()
            false
        } else {
            true
        }
    }

    fun onSuccessfulCheckin() {
        amountOfTestsRun = zero
    }

    fun onSettings(settings: Settings) {
        this.settings = settings
    }

    data class Settings(val enabled: Boolean)

    @Suppress("unused")
    private data class Amount(val n: Int) {
        operator fun inc() = Amount(n + 1)
        companion object {
            val zero = Amount(0)
        }
    }
}

class LimboComponent(project: Project) : AbstractProjectComponent(project) {
    private val unitTestsWatcher = UnitTestsWatcher(myProject)
    private lateinit var limbo: Limbo

    override fun projectOpened() {
        val ide = Ide(myProject, IdeFromAutoRevert(myProject))
        val settings = ServiceManager.getService(LimitedWipSettings::class.java)
        limbo = Limbo(ide, settings.toLimboSettings())
        ide.limbo = limbo

        unitTestsWatcher.start(object : UnitTestsWatcher.Listener {
            override fun onUnitTestSucceeded() = limbo.onUnitTestSucceeded()
            override fun onUnitTestFailed() = limbo.onUnitTestFailed()
        })

        LimitedWipCheckin.registerListener(myProject, object : LimitedWipCheckin.Listener {
            override fun onSuccessfulCheckin(allFileAreCommitted: Boolean) = limbo.onSuccessfulCheckin()
        })

        LimitedWipConfigurable.registerSettingsListener(myProject, object : LimitedWipConfigurable.Listener {
            override fun onSettingsUpdate(settings: LimitedWipSettings) {
                limbo.onSettings(settings.toLimboSettings())
            }
        })
    }

    fun isCommitAllowed(): Boolean = limbo.isCommitAllowed()

    private fun LimitedWipSettings.toLimboSettings() = Limbo.Settings(enabled = this.limboEnabled)
}