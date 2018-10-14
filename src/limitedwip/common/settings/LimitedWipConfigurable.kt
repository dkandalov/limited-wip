package limitedwip.common.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.util.Disposer
import limitedwip.common.pluginDisplayName
import limitedwip.common.pluginId
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class LimitedWipConfigurable : SearchableConfigurable {
    private var settingsForm: SettingsForm? = null

    override fun createComponent(): JComponent? {
        val settings = ServiceManager.getService(LimitedWipSettings::class.java)
        settingsForm = SettingsForm(settings)
        return settingsForm!!.root
    }

    override fun apply() {
        val newSettings = settingsForm!!.applyChanges()
        Extensions.getRootArea().getExtensionPoint<Listener>(extensionPointName).extensions.forEach { listener ->
            listener.onSettingsUpdate(newSettings)
        }
    }

    override fun reset() {
        settingsForm!!.resetChanges()
        settingsForm!!.updateUIFromState()
    }

    override fun disposeUIResources() {
        settingsForm = null
    }

    override fun isModified() = settingsForm != null && settingsForm!!.isModified

    @Nls override fun getDisplayName() = pluginDisplayName

    override fun getId(): String = pluginDisplayName

    override fun enableSearch(option: String?): Runnable? = null

    override fun getHelpTopic(): String? = null

    interface Listener {
        fun onSettingsUpdate(settings: LimitedWipSettings)
    }

    companion object {
        private const val extensionPointName = "$pluginId.settingsListener"

        fun registerSettingsListener(disposable: Disposable, listener: Listener) {
            val extensionPoint = Extensions.getRootArea().getExtensionPoint<Listener>(extensionPointName)
            extensionPoint.registerExtension(listener)
            Disposer.register(disposable, Disposable { extensionPoint.unregisterExtension(listener) })
        }
    }
}
