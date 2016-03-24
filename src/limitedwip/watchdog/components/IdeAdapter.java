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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import limitedwip.common.LimitedWIPAppComponent;
import limitedwip.watchdog.ChangeSize;
import limitedwip.watchdog.Watchdog;
import limitedwip.watchdog.ui.WatchdogStatusBarWidget;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class IdeAdapter {
	private final WatchdogStatusBarWidget watchdogWidget = new WatchdogStatusBarWidget();
	private final Project project;
	private Watchdog.Settings settings;
	private ChangeSize lastChangeSize = new ChangeSize(0);
	private int skipChecks;
	private Notification lastNotification;


	public IdeAdapter(Project project) {
		this.project = project;
	}

	public ChangeSize currentChangeListSizeInLines() {
		if (skipChecks > 0) {
			skipChecks--;
			return lastChangeSize;
		}
		ChangeSize changeSize = ApplicationManager.getApplication().runReadAction(new Computable<ChangeSize>() {
			@Override public ChangeSize compute() {
				return ChangeSizeProjectComponent.getInstance(project).currentChangeListSizeInLines();
			}
		});
		if (changeSize.isApproximate) {
			changeSize = new ChangeSize(lastChangeSize.value, true);
			skipChecks = 10;
		}
		lastChangeSize = changeSize;
		return changeSize;
	}

	public void showCurrentChangeListSize(ChangeSize linesInChange, int maxLinesInChange) {
		watchdogWidget.showChangeSize(asString(linesInChange), maxLinesInChange);
		updateStatusBar();
	}

	public void onSettingsUpdate(Watchdog.Settings settings) {
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
		boolean shouldShowWatchdog = settings.enabled && settings.showRemainingChangesInToolbar;
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

		if (lastNotification != null && !lastNotification.isExpired()) {
			lastNotification.expire();
		}
		lastNotification = notification;
	}

	public void onChangeSizeWithinLimit() {
		if (lastNotification != null && !lastNotification.isExpired()) {
			lastNotification.expire();
			lastNotification = null;
		}
	}

    private static String asString(ChangeSize changeSize) {
        return changeSize.isApproximate ? "≈" + changeSize.value : String.valueOf(changeSize.value);
    }
}
