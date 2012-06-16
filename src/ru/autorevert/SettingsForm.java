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
	private JCheckBox showTimerInToolbarCheckBox;
	private JCheckBox notImplementedCheckBox;
	private JCheckBox notImplementedCheckBox1;

	private final Settings initialState;
	private Settings currentState;
	private boolean isUpdatingUI;

	public SettingsForm(Settings initialState) {
		this.initialState = initialState;
		this.currentState = new Settings();
		currentState.loadState(initialState);
		updateUIFromState();

		ActionListener commonActionListener = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				updateStateFromUI();
				updateUIFromState();
			}
		};
		minutesTillRevertComboBox.addActionListener(commonActionListener);
		showTimerInToolbarCheckBox.addActionListener(commonActionListener);
	}

	public void updateUIFromState() {
		if (isUpdatingUI) return;
		isUpdatingUI = true;

		minutesTillRevertComboBox.setSelectedItem(String.valueOf(currentState.minutesTillRevert));
		showTimerInToolbarCheckBox.setSelected(currentState.showTimerInToolbar);

		isUpdatingUI = false;
	}

	private void updateStateFromUI() {
		try {
			Integer value = Integer.valueOf((String) minutesTillRevertComboBox.getSelectedItem());
			if (value >= Settings.MIN_MINUTES_TO_REVERT && value <= Settings.MAX_MINUTES_TO_REVERT) {
				currentState.minutesTillRevert = value;
			}
			currentState.showTimerInToolbar = showTimerInToolbarCheckBox.isSelected();
		} catch (NumberFormatException ignored) {
		}
	}

	public boolean isModified() {
		return !currentState.equals(initialState);
	}

	public Settings applyChanges() {
		initialState.loadState(currentState);
		return initialState;
	}

	public void resetChanges() {
		currentState.loadState(initialState);
	}
}
