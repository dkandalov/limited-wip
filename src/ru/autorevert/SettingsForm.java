package ru.autorevert;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: dima
 * Date: 14/06/2012
 */
public class SettingsForm {
	private JComboBox secondsTillRevertComboBox;
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
		
		secondsTillRevertComboBox.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				uiState.minutesTillRevert = Integer.valueOf((String) secondsTillRevertComboBox.getModel().getSelectedItem());
			}
		});
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
