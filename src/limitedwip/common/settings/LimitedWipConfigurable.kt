package limitedwip.common.settings

import com.intellij.openapi.options.SearchableConfigurable
import limitedwip.common.pluginDisplayName
import limitedwip.common.pluginId
import javax.swing.JComponent

class LimitedWipConfigurable : SearchableConfigurable {
    private lateinit var settingsForm: SettingsForm

    override fun createComponent(): JComponent? {
        settingsForm = SettingsForm(LimitedWipSettings.getInstance())
        return settingsForm.root
    }

    override fun apply() {
        settingsForm.applyChanges()
    }

    override fun reset() {
        settingsForm.resetChanges()
        settingsForm.updateUIFromState()
    }

    override fun isModified() = settingsForm.isModified()

    override fun getDisplayName() = pluginDisplayName

    override fun getId(): String = pluginId
}
