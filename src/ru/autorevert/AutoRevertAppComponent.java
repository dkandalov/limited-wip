package ru.autorevert;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * User: dima
 * Date: 14/06/2012
 */
public class AutoRevertAppComponent implements ApplicationComponent, Configurable {
	@Override public void initComponent() {
	}

	@Override public void disposeComponent() {
	}

	@Override public JComponent createComponent() {
		return null;
	}

	@Override public boolean isModified() {
		return false;
	}

	@Override public void apply() throws ConfigurationException {
	}

	@Override public void reset() {
	}

	@Override public void disposeUIResources() {
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
