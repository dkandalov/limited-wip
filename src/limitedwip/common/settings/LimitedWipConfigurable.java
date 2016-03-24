package limitedwip.common.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.Disposer;
import limitedwip.common.PluginId;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LimitedWipConfigurable implements SearchableConfigurable {
	private static final String EXTENSION_POINT_NAME = PluginId.value + ".settingsListener";
	private SettingsForm settingsForm;


	@Nullable @Override public JComponent createComponent() {
		LimitedWIPSettings settings = ServiceManager.getService(LimitedWIPSettings.class);
		settingsForm = new SettingsForm(settings);
		return settingsForm.root;
	}

	@Override public boolean isModified() {
		return settingsForm != null && settingsForm.isModified();
	}

	@Override public void apply() throws ConfigurationException {
		LimitedWIPSettings newSettings = settingsForm.applyChanges();
		notifySettingsListeners(newSettings);
	}

	@Override public void reset() {
		settingsForm.resetChanges();
		settingsForm.updateUIFromState();
	}

	@Override public void disposeUIResources() {
		settingsForm = null;
	}

	@Nls @Override public String getDisplayName() {
		return PluginId.displayName;
	}

	@NotNull @Override public String getId() {
		return PluginId.displayName;
	}

	@Nullable @Override public Runnable enableSearch(String option) {
		return null;
	}

	@Nullable @Override public String getHelpTopic() {
		return null;
	}

	private void notifySettingsListeners(LimitedWIPSettings settings) {
		final ExtensionPoint<Listener> extensionPoint = Extensions.getRootArea().getExtensionPoint(EXTENSION_POINT_NAME);
		for (Listener listener : extensionPoint.getExtensions()) {
			listener.onSettingsUpdate(settings);
		}
	}

	public static void registerSettingsListener(Disposable disposable, final Listener listener) {
		final ExtensionPoint<Listener> extensionPoint = Extensions.getRootArea().getExtensionPoint(EXTENSION_POINT_NAME);
		extensionPoint.registerExtension(listener);
		Disposer.register(disposable, new Disposable() {
			@Override public void dispose() {
				extensionPoint.unregisterExtension(listener);
			}
		});
	}

	public interface Listener {
		void onSettingsUpdate(LimitedWIPSettings settings);
	}
}
