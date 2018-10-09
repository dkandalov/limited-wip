package limitedwip.common.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.util.Disposer
import limitedwip.common.PluginId
import org.jetbrains.annotations.Nls

import javax.swing.*

class LimitedWipConfigurable : SearchableConfigurable {
    private var settingsForm: SettingsForm? = null

    override fun createComponent(): JComponent? {
        val settings = ServiceManager.getService(LimitedWIPSettings::class.java)
        settingsForm = SettingsForm(settings)
        return settingsForm!!.root
    }

    override fun apply() {
        val newSettings = settingsForm!!.applyChanges()
        notifySettingsListeners(newSettings)
    }

    override fun reset() {
        settingsForm!!.resetChanges()
        settingsForm!!.updateUIFromState()
    }

    override fun disposeUIResources() {
        settingsForm = null
    }

    override fun isModified() = settingsForm != null && settingsForm!!.isModified

    @Nls override fun getDisplayName() = PluginId.displayName

    override fun getId(): String = PluginId.displayName

    override fun enableSearch(option: String?): Runnable? = null

    override fun getHelpTopic(): String? = null

    private fun notifySettingsListeners(settings: LimitedWIPSettings) {
        val extensionPoint = Extensions.getRootArea().getExtensionPoint<Listener>(EXTENSION_POINT_NAME)
        for (listener in extensionPoint.extensions) {
            listener.onSettingsUpdate(settings)
        }
    }

    interface Listener {
        fun onSettingsUpdate(settings: LimitedWIPSettings)
    }

    companion object {
        private const val EXTENSION_POINT_NAME = PluginId.value + ".settingsListener"

        fun registerSettingsListener(disposable: Disposable, listener: Listener) {
            val extensionPoint = Extensions.getRootArea().getExtensionPoint<Listener>(EXTENSION_POINT_NAME)
            extensionPoint.registerExtension(listener)
            Disposer.register(disposable, Disposable { extensionPoint.unregisterExtension(listener) })
        }
    }
}
