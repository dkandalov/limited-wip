package limitedwip.common.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import limitedwip.common.pluginDisplayName
import limitedwip.common.pluginId
import javax.swing.JComponent

class LimitedWipConfigurable(val project: Project) : SearchableConfigurable {
    private lateinit var settingsForm: SettingsForm

    override fun createComponent(): JComponent? {
        settingsForm = SettingsForm(LimitedWipSettings.getInstance(project))
        return settingsForm.root
    }

    override fun apply() {
        settingsForm.applyChanges()
    }

    override fun reset() {
        settingsForm.resetChanges()
    }

    override fun isModified() = settingsForm.isModified()

    override fun getDisplayName() = pluginDisplayName

    override fun getId(): String = pluginId
}
