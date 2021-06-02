package limitedwip.common.settings

import com.intellij.ide.ui.OptionsSearchTopHitProvider
import com.intellij.ide.ui.search.BooleanOptionDescription
import com.intellij.ide.ui.search.OptionDescription
import com.intellij.openapi.project.Project
import limitedwip.common.pluginId
import kotlin.reflect.KMutableProperty0

class LimitedWipTopHitProvider: OptionsSearchTopHitProvider.ProjectLevelProvider {
    override fun getId() = pluginId

    override fun getOptions(project: Project): Collection<OptionDescription> {
        val settings = LimitedWipSettings.getInstance(project)
        return listOf(
            Option("Change size watchdog enabled", settings::watchdogEnabled, settings),
            Option("Show remaining changes in toolbar", settings::showRemainingChangesInToolbar, settings),
            Option("No commits above threshold", settings::noCommitsAboveThreshold, settings),

            Option("Auto-revert enabled", settings::autoRevertEnabled, settings),
            Option("Show time till revert", settings::showTimerInToolbar, settings),

            Option("TCR mode enabled", settings::tcrEnabled, settings)
        )
    }

    private class Option(
        optionName: String,
        val property: KMutableProperty0<Boolean>,
        val settings: LimitedWipSettings
    ): BooleanOptionDescription(optionName, pluginId) {
        override fun isOptionEnabled() = property()
        override fun setOptionState(enabled: Boolean) {
            property.set(enabled)
            settings.notifyListeners()
        }
    }
}
