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
package limitedwip.ui.settings;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SettingsForm {
	public JPanel root;

	private JCheckBox watchdogEnabled;
	private JComboBox maxLinesInChange;
	private JComboBox notificationInterval;
	private JCheckBox showRemainingInToolbar;
	private JCheckBox disableCommitsAboveThreshold;

	private JCheckBox autoRevertEnabled;
	private JComboBox minutesTillRevert;
	private JCheckBox notifyOnRevert;
	private JCheckBox showTimerInToolbar;

	private final Settings initialState;
	private Settings currentState;
	private boolean isUpdatingUI;

	public SettingsForm(Settings initialState) {
		this.initialState = initialState;
		this.currentState = new Settings();
		currentState.loadState(initialState);
		updateUIFromState();

		ActionListener commonActionListener = new ActionListener() {
			@Override public void actionPerformed(@NotNull ActionEvent event) {
				updateStateFromUI();
				updateUIFromState();
			}
		};

		watchdogEnabled.addActionListener(commonActionListener);
		maxLinesInChange.addActionListener(commonActionListener);
		notificationInterval.addActionListener(commonActionListener);
		showRemainingInToolbar.addActionListener(commonActionListener);
		disableCommitsAboveThreshold.addActionListener(commonActionListener);

		autoRevertEnabled.addActionListener(commonActionListener);
		minutesTillRevert.addActionListener(commonActionListener);
		notifyOnRevert.addActionListener(commonActionListener);
		showTimerInToolbar.addActionListener(commonActionListener);
	}

	public void updateUIFromState() {
		if (isUpdatingUI) return;
		isUpdatingUI = true;

		watchdogEnabled.setSelected(currentState.watchdogEnabled);
		maxLinesInChange.setSelectedItem(String.valueOf(currentState.maxLinesInChange));
		notificationInterval.setSelectedItem(String.valueOf(currentState.notificationIntervalInMinutes));
		showRemainingInToolbar.setSelected(currentState.showRemainingChangesInToolbar);
		disableCommitsAboveThreshold.setSelected(currentState.disableCommitsAboveThreshold);

		autoRevertEnabled.setSelected(currentState.autoRevertEnabled);
		minutesTillRevert.setSelectedItem(String.valueOf(currentState.minutesTillRevert));
		notifyOnRevert.setSelected(currentState.notifyOnRevert);
		showTimerInToolbar.setSelected(currentState.showTimerInToolbar);

		minutesTillRevert.setEnabled(currentState.autoRevertEnabled);
		notifyOnRevert.setEnabled(currentState.autoRevertEnabled);
		showTimerInToolbar.setEnabled(currentState.autoRevertEnabled);
		maxLinesInChange.setEnabled(currentState.watchdogEnabled);
		notificationInterval.setEnabled(currentState.watchdogEnabled);
		showRemainingInToolbar.setEnabled(currentState.watchdogEnabled);
		disableCommitsAboveThreshold.setEnabled(currentState.watchdogEnabled);

		isUpdatingUI = false;
	}

	private void updateStateFromUI() {
		try {
			currentState.watchdogEnabled = watchdogEnabled.isSelected();
			Integer lineCount = Integer.valueOf((String) maxLinesInChange.getSelectedItem());
			if (Settings.changedLinesRange.isWithin(lineCount)) {
				currentState.maxLinesInChange = lineCount;
			}
			Integer minutes = Integer.valueOf((String) notificationInterval.getSelectedItem());
			if (Settings.notificationIntervalRange.isWithin(minutes)) {
				currentState.notificationIntervalInMinutes = minutes;
			}
			currentState.showRemainingChangesInToolbar = showRemainingInToolbar.isSelected();
			currentState.disableCommitsAboveThreshold = disableCommitsAboveThreshold.isSelected();

			currentState.autoRevertEnabled = autoRevertEnabled.isSelected();
			minutes = Integer.valueOf((String) minutesTillRevert.getSelectedItem());
			if (Settings.minutesToRevertRange.isWithin(minutes)) {
				currentState.minutesTillRevert = minutes;
			}
			currentState.notifyOnRevert = notifyOnRevert.isSelected();
			currentState.showTimerInToolbar = showTimerInToolbar.isSelected();

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
