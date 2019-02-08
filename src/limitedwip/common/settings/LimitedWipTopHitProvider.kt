package limitedwip.common.settings

import com.intellij.ide.ui.OptionsTopHitProvider
import com.intellij.ide.ui.search.BooleanOptionDescription
import com.intellij.ide.ui.search.OptionDescription
import com.intellij.openapi.project.Project
import limitedwip.common.pluginId
import kotlin.reflect.KMutableProperty0

class LimitedWipTopHitProvider : OptionsTopHitProvider() {
    private val settings = LimitedWipSettings.getInstance()

    override fun getId() = pluginId

    override fun getOptions(project: Project?): Collection<OptionDescription> =
        listOf(
            Option("Change size watchdog enabled", settings::watchdogEnabled),
            Option("Show remaining changes in toolbar", settings::showRemainingChangesInToolbar),
            Option("No commits above threshold", settings::noCommitsAboveThreshold),

            Option("Auto-revert enabled", settings::autoRevertEnabled),
            Option("Show time till revert", settings::showTimerInToolbar),

            Option("TCR mode enabled", settings::tcrEnabled)
        )

    private inner class Option(
        optionName: String,
        val property: KMutableProperty0<Boolean>
    ) : BooleanOptionDescription(optionName, pluginId) {
        override fun isOptionEnabled() = property()
        override fun setOptionState(enabled: Boolean) {
            property.set(enabled)
            settings.notifyListeners()
        }
    }
}
