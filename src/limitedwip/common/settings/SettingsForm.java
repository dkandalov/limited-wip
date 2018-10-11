package limitedwip.common.settings;

import com.intellij.ide.BrowserUtil;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
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
	private LinkLabel<Void> openReadme;

	private final LimitedWIPSettings initialState;
	private LimitedWIPSettings currentState;
	private boolean isUpdatingUI;

	public SettingsForm(LimitedWIPSettings initialState) {
		this.initialState = initialState;
		this.currentState = new LimitedWIPSettings();
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

		openReadme.setListener(new LinkListener<Void>() {
			@Override public void linkSelected(LinkLabel aSource, Void aLinkData) {
				BrowserUtil.open("https://github.com/dkandalov/limited-wip/blob/master/README.md#limited-wip");
			}
		}, null);
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
			if (LimitedWIPSettings.changedLinesRange.isWithin(lineCount)) {
				currentState.maxLinesInChange = lineCount;
			}
			Integer minutes = Integer.valueOf((String) notificationInterval.getSelectedItem());
			if (LimitedWIPSettings.notificationIntervalRange.isWithin(minutes)) {
				currentState.notificationIntervalInMinutes = minutes;
			}
			currentState.showRemainingChangesInToolbar = showRemainingInToolbar.isSelected();
			currentState.disableCommitsAboveThreshold = disableCommitsAboveThreshold.isSelected();

			currentState.autoRevertEnabled = autoRevertEnabled.isSelected();
			minutes = Integer.valueOf((String) minutesTillRevert.getSelectedItem());
			if (LimitedWIPSettings.minutesToRevertRange.isWithin(minutes)) {
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

	public LimitedWIPSettings applyChanges() {
		initialState.loadState(currentState);
		return initialState;
	}

	public void resetChanges() {
		currentState.loadState(initialState);
	}
}
