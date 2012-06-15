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

	public final Settings initialState;
	public Settings currentState;

	public SettingsForm(Settings initialState) {
		this.initialState = initialState;
		this.currentState = new Settings();
		currentState.loadState(initialState);

		minutesTillRevertComboBox.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				updateStateFromUI();
				updateUIFromState();
			}
		});
	}

	private void updateUIFromState() {
		minutesTillRevertComboBox.getModel().setSelectedItem(currentState.minutesTillRevert);
	}

	private void updateStateFromUI() {
		try {
			Integer value = Integer.valueOf((String) minutesTillRevertComboBox.getSelectedItem());
			if (value > 0) {
				currentState.minutesTillRevert = value;
			}
		} catch (NumberFormatException ignored) {
		}
	}
}
