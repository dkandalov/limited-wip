package ru.autorevert;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * User: dima
 * Date: 14/06/2012
 */
public class AutoRevertAppComponent implements ApplicationComponent, Configurable {

	private SettingsForm settingsForm;

	@Override public void initComponent() {
		Settings settings = ServiceManager.getService(Settings.class);

		for (Project project : ProjectManager.getInstance().getOpenProjects()) {
			AutoRevertProjectComponent autoRevertProjectComponent = project.getComponent(AutoRevertProjectComponent.class);
			autoRevertProjectComponent.onNewSettings(settings);
		}
	}

	@Override public void disposeComponent() {
	}

	@Override public JComponent createComponent() {
		Settings settings = ServiceManager.getService(Settings.class);
		settingsForm = new SettingsForm(settings);
		return settingsForm.root;
	}

	@Override public boolean isModified() {
		return settingsForm.isModfied();
	}

	@Override public void apply() throws ConfigurationException {
		settingsForm.apply();
	}

	@Override public void reset() {
		settingsForm.reset();
	}

	@Override public void disposeUIResources() {
		settingsForm = null;
	}

	@Nls @Override public String getDisplayName() {
		return "Auto-revert settings";
	}

	@NotNull @Override public String getComponentName() {
		return "AutoRevertAppComponent";
	}

	@Override public Icon getIcon() {
		return null;
	}

	@Override public String getHelpTopic() {
		return null;
	}
}
