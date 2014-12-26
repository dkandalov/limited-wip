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
package limitedwip.settings;

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
	private JCheckBox disableCommitsWithErrorsCheckBox;

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
		disableCommitsWithErrorsCheckBox.addActionListener(commonActionListener);
	}

	public void updateUIFromState() {
		if (isUpdatingUI) return;
		isUpdatingUI = true;

		minutesTillRevertComboBox.setSelectedItem(String.valueOf(currentState.minutesTillRevert));
		showTimerInToolbarCheckBox.setSelected(currentState.showTimerInToolbar);
		disableCommitsWithErrorsCheckBox.setSelected(currentState.disableCommitsWithErrors);

		isUpdatingUI = false;
	}

	private void updateStateFromUI() {
		try {
			Integer value = Integer.valueOf((String) minutesTillRevertComboBox.getSelectedItem());
			if (value >= Settings.MIN_MINUTES_TO_REVERT && value <= Settings.MAX_MINUTES_TO_REVERT) {
				currentState.minutesTillRevert = value;
			}
			currentState.showTimerInToolbar = showTimerInToolbarCheckBox.isSelected();
			currentState.disableCommitsWithErrors = disableCommitsWithErrorsCheckBox.isSelected();
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
