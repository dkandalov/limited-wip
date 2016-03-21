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
package limitedwip.watchdog.components;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import limitedwip.common.LimitedWIPSettings;
import limitedwip.common.components.LimitedWIPAppComponent;
import limitedwip.watchdog.ui.WatchdogStatusBarWidget;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class IdeNotifications {
	private final WatchdogStatusBarWidget watchdogWidget = new WatchdogStatusBarWidget();
	private final Project project;
	private LimitedWIPSettings settings;


	public IdeNotifications(Project project, LimitedWIPSettings settings) {
		this.project = project;
		this.settings = settings;

		onSettingsUpdate(settings);
	}

	public void currentChangeListSize(ChangeSize linesInChange, int maxLinesInChange) {
		watchdogWidget.showChangeSize(asString(linesInChange), maxLinesInChange);
		updateStatusBar();
	}

	public void onSettingsUpdate(LimitedWIPSettings settings) {
		this.settings = settings;
		updateStatusBar();
	}

    public void onSkipNotificationUntilCommit(boolean value) {
        String stateDescription = value ? "disabled till next commit" : "enabled";
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

	public void onChangeSizeTooBig(ChangeSize linesChanged, int changedLinesLimit) {
		// TODO limit number of notifications displayed at the same time
		NotificationListener listener = new NotificationListener() {
			@Override public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
				WatchdogComponent watchdogComponent = project.getComponent(WatchdogComponent.class);
                assert watchdogComponent != null;
                watchdogComponent.skipNotificationsUntilCommit(true);
				notification.expire();
			}
		};

		Notification notification = new Notification(
				LimitedWIPAppComponent.displayName,
				"Change Size Exceeded Limit",
				"Lines changed: " + asString(linesChanged) + "; " +
					"limit: " + changedLinesLimit + "<br/>" +
					"Please consider committing, splitting or reverting changes<br/>" +
					"(<a href=\"\">Click here</a> to skip notifications till next commit)",
				NotificationType.WARNING,
				listener
		);
		project.getMessageBus().syncPublisher(Notifications.TOPIC).notify(notification);
	}

    private static String asString(ChangeSize changeSize) {
        return changeSize.isApproximate ? "≈" + changeSize.value : String.valueOf(changeSize.value);
    }
}
