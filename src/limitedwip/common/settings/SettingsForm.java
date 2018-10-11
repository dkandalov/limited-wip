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

	private final LimitedWipSettings initialState;
	private LimitedWipSettings currentState;
	private boolean isUpdatingUI;

	public SettingsForm(LimitedWipSettings initialState) {
		this.initialState = initialState;
		this.currentState = new LimitedWipSettings();
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

		watchdogEnabled.setSelected(currentState.getWatchdogEnabled());
		maxLinesInChange.setSelectedItem(String.valueOf(currentState.getMaxLinesInChange()));
		notificationInterval.setSelectedItem(String.valueOf(currentState.getNotificationIntervalInMinutes()));
		showRemainingInToolbar.setSelected(currentState.getShowRemainingChangesInToolbar());
		disableCommitsAboveThreshold.setSelected(currentState.getDisableCommitsAboveThreshold());

		autoRevertEnabled.setSelected(currentState.getAutoRevertEnabled());
		minutesTillRevert.setSelectedItem(String.valueOf(currentState.getMinutesTillRevert()));
		notifyOnRevert.setSelected(currentState.getNotifyOnRevert());
		showTimerInToolbar.setSelected(currentState.getShowTimerInToolbar());

		minutesTillRevert.setEnabled(currentState.getAutoRevertEnabled());
		notifyOnRevert.setEnabled(currentState.getAutoRevertEnabled());
		showTimerInToolbar.setEnabled(currentState.getAutoRevertEnabled());
		maxLinesInChange.setEnabled(currentState.getWatchdogEnabled());
		notificationInterval.setEnabled(currentState.getWatchdogEnabled());
		showRemainingInToolbar.setEnabled(currentState.getWatchdogEnabled());
		disableCommitsAboveThreshold.setEnabled(currentState.getWatchdogEnabled());

		isUpdatingUI = false;
	}

	private void updateStateFromUI() {
		try {
			currentState.setWatchdogEnabled(watchdogEnabled.isSelected());
			Integer lineCount = Integer.valueOf((String) maxLinesInChange.getSelectedItem());
			if (LimitedWipSettings.Companion.getChangedLinesRange().isWithin(lineCount)) {
				currentState.setMaxLinesInChange(lineCount);
			}
			Integer minutes = Integer.valueOf((String) notificationInterval.getSelectedItem());
			if (LimitedWipSettings.Companion.getNotificationIntervalRange().isWithin(minutes)) {
				currentState.setNotificationIntervalInMinutes(minutes);
			}
			currentState.setShowRemainingChangesInToolbar(showRemainingInToolbar.isSelected());
			currentState.setDisableCommitsAboveThreshold(disableCommitsAboveThreshold.isSelected());

			currentState.setAutoRevertEnabled(autoRevertEnabled.isSelected());
			minutes = Integer.valueOf((String) minutesTillRevert.getSelectedItem());
			if (LimitedWipSettings.Companion.getMinutesToRevertRange().isWithin(minutes)) {
				currentState.setMinutesTillRevert(minutes);
			}
			currentState.setNotifyOnRevert(notifyOnRevert.isSelected());
			currentState.setShowTimerInToolbar(showTimerInToolbar.isSelected());

		} catch (NumberFormatException ignored) {
		}
	}

	public boolean isModified() {
		return !currentState.equals(initialState);
	}

	public LimitedWipSettings applyChanges() {
		initialState.loadState(currentState);
		return initialState;
	}

	public void resetChanges() {
		currentState.loadState(initialState);
	}
}
