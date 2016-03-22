package limitedwip.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import limitedwip.autorevert.components.AutoRevertComponent;
import limitedwip.common.ui.SettingsForm;
import limitedwip.watchdog.components.WatchdogComponent;
import limitedwip.watchdog.components.DisableLargeCommitsAppComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LimitedWipConfigurable implements SearchableConfigurable {
	private SettingsForm settingsForm;

	@Nullable @Override public JComponent createComponent() {
		LimitedWIPSettings settings = ServiceManager.getService(LimitedWIPSettings.class);
		settingsForm = new SettingsForm(settings);
		return settingsForm.root;
	}

	@Override public boolean isModified() {
		return settingsForm.isModified();
	}

	@Override public void apply() throws ConfigurationException {
		LimitedWIPSettings newSettings = settingsForm.applyChanges();
		notifySettingsListeners(newSettings);
	}

	private void notifySettingsListeners(LimitedWIPSettings settings) {
		for (Project project : ProjectManager.getInstance().getOpenProjects()) {
			project.getComponent(WatchdogComponent.class).onSettingsUpdate(settings);
			project.getComponent(AutoRevertComponent.class).onSettingsUpdate(settings);
		}

		ApplicationManager.getApplication()
				.getComponent(DisableLargeCommitsAppComponent.class)
				.onSettingsUpdate(settings);
	}

	@Override public void reset() {
		settingsForm.resetChanges();
		settingsForm.updateUIFromState();
	}

	@Override public void disposeUIResources() {
		settingsForm = null;
	}

	@Nls @Override public String getDisplayName() {
		return LimitedWIPAppComponent.displayName;
	}

	@NotNull @Override public String getId() {
		return LimitedWIPAppComponent.displayName;
	}

	@Nullable @Override public Runnable enableSearch(String option) {
		return null;
	}

	@Nullable @Override public String getHelpTopic() {
		return null;
	}
}
