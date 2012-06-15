package ru.autorevert;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: dima
 * Date: 14/06/2012
 */
public class SettingsForm {
	public JPanel root;
	private JComboBox minutesTillRevertComboBox;
	private JCheckBox TODOCheckBox;
	private JCheckBox TODOCheckBox1;
	private JCheckBox TODOCheckBox2;
	private JCheckBox TODOCheckBox3;

	private final Settings initialState;
	private Settings currentState;

	public SettingsForm(Settings initialState) {
		this.initialState = initialState;
		this.currentState = new Settings();
		currentState.loadState(initialState);

		minutesTillRevertComboBox.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				try {
					currentState.minutesTillRevert = Integer.valueOf((String) minutesTillRevertComboBox.getModel().getSelectedItem());
				} catch (NumberFormatException e1) {
					minutesTillRevertComboBox.getModel().setSelectedItem(currentState.minutesTillRevert);
				}
			}
		});
	}

	public boolean isModfied() {
		return currentState.equals(initialState);
	}

	public void apply() {
		// TODO implement

	}

	public void reset() {
		currentState.loadState(initialState);
	}
}
