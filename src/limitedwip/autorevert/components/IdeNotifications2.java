package limitedwip.autorevert.components;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import limitedwip.autorevert.ui.AutoRevertStatusBarWidget;
import limitedwip.common.LimitedWIPSettings;
import limitedwip.common.components.LimitedWIPAppComponent;

public class IdeNotifications2 {
	private final AutoRevertStatusBarWidget autoRevertWidget = new AutoRevertStatusBarWidget();
	private final Project project;
	private LimitedWIPSettings settings;


	public IdeNotifications2(Project project, LimitedWIPSettings settings) {
		this.project = project;
		this.settings = settings;

		onSettingsUpdate(settings);
	}

	public void onProjectClosed() {
		StatusBar statusBar = statusBarFor(project);
		if (statusBar != null) {
			autoRevertWidget.showStoppedText();
			statusBar.removeWidget(autoRevertWidget.ID());
			statusBar.updateWidget(autoRevertWidget.ID());
		}
	}

	public void onAutoRevertStarted(int timeEventsTillRevert) {
		if (settings.showTimerInToolbar) {
			autoRevertWidget.showTime(formatTime(timeEventsTillRevert));
		} else {
			autoRevertWidget.showStartedText();
		}
		updateStatusBar();
	}

	public void onAutoRevertStopped() {
		autoRevertWidget.showStoppedText();
		updateStatusBar();
	}

	public void onChangesRevert() {
		Notification notification = new Notification(
				LimitedWIPAppComponent.displayName,
				"Current change list was auto-reverted",
				"(to disable it use widget in the bottom toolbar)",
				NotificationType.WARNING
		);
		project.getMessageBus().syncPublisher(Notifications.TOPIC).notify(notification);
	}

	public void onCommit(int timeEventsTillRevert) {
		if (settings.showTimerInToolbar) {
			autoRevertWidget.showTime(formatTime(timeEventsTillRevert));
		} else {
			autoRevertWidget.showStartedText();
		}
		updateStatusBar();
	}

	public void onTimeTillRevert(int secondsLeft) {
		if (settings.showTimerInToolbar) {
			autoRevertWidget.showTime(formatTime(secondsLeft));
		} else {
			autoRevertWidget.showStartedText();
		}
		updateStatusBar();
	}

	public void onSettingsUpdate(LimitedWIPSettings settings) {
		this.settings = settings;
		updateStatusBar();
	}

	private void updateStatusBar() {
		StatusBar statusBar = statusBarFor(project);
		if (statusBar == null) return;

		boolean hasAutoRevertWidget = statusBar.getWidget(autoRevertWidget.ID()) != null;
		if (hasAutoRevertWidget && settings.autoRevertEnabled) {
			statusBar.updateWidget(autoRevertWidget.ID());

		} else if (hasAutoRevertWidget) {
			statusBar.removeWidget(autoRevertWidget.ID());

		} else if (settings.autoRevertEnabled) {
			autoRevertWidget.showStoppedText();
			statusBar.addWidget(autoRevertWidget, "before Position");
			statusBar.updateWidget(autoRevertWidget.ID());
		}
	}

	private static StatusBar statusBarFor(Project project) {
		return WindowManager.getInstance().getStatusBar(project);
	}

	private static String formatTime(int seconds) {
		int min = seconds / 60;
		int sec = seconds % 60;
		return String.format("%02d", min) + ":" + String.format("%02d", sec);
	}
}

