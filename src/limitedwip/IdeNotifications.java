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
package limitedwip;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import limitedwip.components.LimitedWIPAppComponent;
import limitedwip.components.LimitedWIPProjectComponent;
import limitedwip.components.VcsIdeUtil;
import limitedwip.ui.AutoRevertStatusBarWidget;
import limitedwip.ui.WatchdogStatusBarWidget;
import limitedwip.ui.settings.Settings;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class IdeNotifications {
	private final AutoRevertStatusBarWidget autoRevertWidget = new AutoRevertStatusBarWidget();
	private final WatchdogStatusBarWidget watchdogWidget = new WatchdogStatusBarWidget();
	private final Project project;
	private Settings settings;


	public IdeNotifications(Project project, Settings settings) {
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

	public void currentChangeListSize(VcsIdeUtil.ChangeSize linesInChange, int maxLinesInChange) {
		watchdogWidget.showChangeSize(linesInChange, maxLinesInChange);
		updateStatusBar();
	}

	public void onSettingsUpdate(Settings settings) {
		this.settings = settings;
		updateStatusBar();
	}

    public void onSkipNotificationUntilCommit(boolean value) {
        String stateDescription = value ? "disabled" : "enabled";
        Notification notification = new Notification(
                LimitedWIPAppComponent.displayName,
                "Change size notifications are " + stateDescription,
                "(use widget in the bottom toolbar to toggle it)",
                NotificationType.INFORMATION
        );
        project.getMessageBus().syncPublisher(Notifications.TOPIC).notify(notification);
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

		boolean hasWatchdogWidget = statusBar.getWidget(watchdogWidget.ID()) != null;
		boolean shouldShowWatchdog = settings.watchdogEnabled && settings.showRemainingChangesInToolbar;
		if (hasWatchdogWidget && shouldShowWatchdog) {
            statusBar.updateWidget(watchdogWidget.ID());

        } else if (hasWatchdogWidget) {
            statusBar.removeWidget(watchdogWidget.ID());

        } else if (shouldShowWatchdog) {
            watchdogWidget.showInitialText(settings.maxLinesInChange);
            statusBar.addWidget(watchdogWidget, "before Position");
            statusBar.updateWidget(watchdogWidget.ID());
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

	public void onChangeSizeTooBig(int linesChanged, int changedLinesLimit) {
		NotificationListener listener = new NotificationListener() {
			@Override public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
				LimitedWIPProjectComponent limitedWIPProjectComponent = project.getComponent(LimitedWIPProjectComponent.class);
                assert limitedWIPProjectComponent != null;
                limitedWIPProjectComponent.skipNotificationsUntilCommit(true);
				notification.expire();
			}
		};

		Notification notification = new Notification(
				LimitedWIPAppComponent.displayName,
				"Change Size Exceeded Limit",
				"Lines changed: " + linesChanged + "; " +
					"limit: " + changedLinesLimit + "<br/>" +
					"Please consider committing, splitting or reverting changes<br/>" +
					"(<a href=\"\">Click here</a> to skip notifications till next commit)",
				NotificationType.WARNING,
				listener
		);
		project.getMessageBus().syncPublisher(Notifications.TOPIC).notify(notification);
	}
}
