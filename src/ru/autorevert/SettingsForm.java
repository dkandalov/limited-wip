package ru.autorevert;

import javax.swing.*;

/**
 * User: dima
 * Date: 14/06/2012
 */
public class SettingsForm {
	private JComboBox comboBox1;
	private JCheckBox TODOCheckBox;
	private JCheckBox TODOCheckBox1;
	private JCheckBox TODOCheckBox2;
	public JPanel root;

	private final Settings settings;

	public SettingsForm(Settings settings) {
		this.settings = settings;
	}

	public boolean isModfied() {
		// TODO implement
		return false;
	}

	public void apply() {
		// TODO implement

	}

	public void reset() {
		// TODO implement

	}
}
