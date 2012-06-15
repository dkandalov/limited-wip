package ru.autorevert;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: dima
 * Date: 14/06/2012
 */
public class SettingsForm {
	private JComboBox minutesTillRevertComboBox;
	private JCheckBox TODOCheckBox;
	private JCheckBox TODOCheckBox1;
	private JCheckBox TODOCheckBox2;
	public JPanel root;
	private JCheckBox TODOCheckBox3;

	private final Settings settings;
	private Settings uiState;

	public SettingsForm(Settings settings) {
		this.settings = settings;
		this.uiState = settings;

		minutesTillRevertComboBox.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				try {
					uiState.minutesTillRevert = Integer.valueOf((String) minutesTillRevertComboBox.getModel().getSelectedItem());
				} catch (NumberFormatException e1) {
					minutesTillRevertComboBox.getModel().setSelectedItem(uiState.minutesTillRevert);
				}
			}
		});
	}

	public boolean isModfied() {
		return uiState.equals(settings);
	}

	public void apply() {
		// TODO implement

	}

	public void reset() {
		// TODO implement

	}
}
