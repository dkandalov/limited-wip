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
package limitedwip.components;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import limitedwip.ui.settings.LimitedWIPSettings;
import limitedwip.ui.settings.SettingsForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class LimitedWIPAppComponent implements ApplicationComponent, SearchableConfigurable {
	public static final String displayName = "Limited WIP";
	private static final String limitedWIPAppComponentId = "LimitedWIPAppComponent";

	private SettingsForm settingsForm;
	private final List<LimitedWIPSettings.Listener> settingsListeners = new ArrayList<LimitedWIPSettings.Listener>();

	@Override public void initComponent() { }

	@Override public void disposeComponent() { }

	@Override public JComponent createComponent() {
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

	public void addSettingsListener(LimitedWIPSettings.Listener listener) {
		settingsListeners.add(listener);
		LimitedWIPSettings settings = ServiceManager.getService(LimitedWIPSettings.class);
		listener.onSettingsUpdate(settings);
	}

	public void removeSettingsListener(LimitedWIPSettings.Listener listener) {
		settingsListeners.remove(listener);
	}

	private void notifySettingsListeners(LimitedWIPSettings newSettings) {
		for (LimitedWIPSettings.Listener listener : settingsListeners) {
			listener.onSettingsUpdate(newSettings);
		}
	}

	@Override public void reset() {
		settingsForm.resetChanges();
		settingsForm.updateUIFromState();
	}

	@Override public void disposeUIResources() {
		settingsForm = null;
	}

	@Nls @Override public String getDisplayName() {
		return displayName;
	}

	@NotNull @Override public String getComponentName() {
		return limitedWIPAppComponentId;
	}

	@Override public String getHelpTopic() {
		return null;
	}

	@NotNull @Override public String getId() {
		return limitedWIPAppComponentId;
	}

	@Nullable @Override public Runnable enableSearch(String option) {
		return null;
	}
}
