/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.autorevert.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import ru.autorevert.settings.Settings;
import ru.autorevert.settings.SettingsForm;

import javax.swing.*;

/**
 * User: dima
 * Date: 14/06/2012
 */
public class AutoRevertAppComponent implements ApplicationComponent, Configurable {
	public static final String DISPLAY_NAME = "VCS auto-revert";

	private SettingsForm settingsForm;

	@Override public void initComponent() {
		Settings settings = ServiceManager.getService(Settings.class);
		notifyAutorevertComponentsAbout(settings);
	}

	@Override public void disposeComponent() {
	}

	@Override public JComponent createComponent() {
		Settings settings = ServiceManager.getService(Settings.class);
		settingsForm = new SettingsForm(settings);
		return settingsForm.root;
	}

	@Override public boolean isModified() {
		return settingsForm.isModified();
	}

	@Override public void apply() throws ConfigurationException {
		Settings newSettings = settingsForm.applyChanges();
		notifyAutorevertComponentsAbout(newSettings);
	}

	@Override public void reset() {
		settingsForm.resetChanges();
		settingsForm.updateUIFromState();
	}

	@Override public void disposeUIResources() {
		settingsForm = null;
	}

	@Nls @Override public String getDisplayName() {
		return DISPLAY_NAME;
	}

	@NotNull @Override public String getComponentName() {
		return "AutoRevertAppComponent";
	}

	@Override public String getHelpTopic() {
		return null;
	}

	private void notifyAutorevertComponentsAbout(Settings settings) {
		for (Project project : ProjectManager.getInstance().getOpenProjects()) {
			AutoRevertProjectComponent autoRevertProjectComponent = project.getComponent(AutoRevertProjectComponent.class);
			autoRevertProjectComponent.onNewSettings(settings);
		}
		DisableCommitsWithErrorsComponent component = ApplicationManager.getApplication().getComponent(DisableCommitsWithErrorsComponent.class);
		component.onNewSettings(settings);
	}
}
