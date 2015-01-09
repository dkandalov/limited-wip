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

import com.intellij.ide.DataManager;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Consumer;
import limitedwip.components.LimitedWIPAppComponent;
import limitedwip.components.LimitedWIPProjectComponent;
import limitedwip.ui.settings.Settings;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.MouseEvent;

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

	public void currentChangeListSize(int linesInChange, int maxLinesInChange) {
		watchdogWidget.showChangeSize(linesInChange, maxLinesInChange);
		updateStatusBar();
	}

	public void onSettingsUpdate(Settings settings) {
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
            statusBar.addWidget(autoRevertWidget);
            statusBar.updateWidget(autoRevertWidget.ID());
        }

		boolean hasWatchdogWidget = statusBar.getWidget(watchdogWidget.ID()) != null;
		if (hasWatchdogWidget && settings.watchdogEnabled) {
            statusBar.updateWidget(watchdogWidget.ID());

        } else if (hasWatchdogWidget) {
            statusBar.removeWidget(watchdogWidget.ID());

        } else if (settings.watchdogEnabled) {
            watchdogWidget.showInitialText(settings.maxLinesInChange);
            statusBar.addWidget(watchdogWidget);
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
				limitedWIPProjectComponent.skipNotificationsUntilCommit(true);
				notification.expire();
			}
		};

		Notification notification = new Notification(
				LimitedWIPAppComponent.displayName,
				"Change Exceeded Limit",
				"Lines changed: " + linesChanged + "; " +
						"Limit: " + changedLinesLimit + "<br/>" +
						"Please consider committing or reverting changes<br/>" +
						"(<a href=\"\">Click here</a> to skip notifications till next commit)",
				NotificationType.WARNING,
				listener
		);
		ApplicationManager.getApplication().getMessageBus().syncPublisher(Notifications.TOPIC).notify(notification);
	}


	private static class WatchdogStatusBarWidget implements StatusBarWidget {
		private static final String textPrefix = "Change size: ";

		private String text = "";

		@Override public void install(@NotNull StatusBar statusBar) {
		}

		@Override public void dispose() {
		}

		public void showChangeSize(int linesInChange, int maxLinesInChange) {
			text = textPrefix + linesInChange + "/" + maxLinesInChange;
		}

		public void showInitialText(int maxLinesInChange) {
			text = textPrefix + "-/" + maxLinesInChange;
		}

		@Override public WidgetPresentation getPresentation(@NotNull PlatformType type) {
			return new StatusBarWidget.TextPresentation() {
				@NotNull @Override public String getText() {
					return text;
				}

				@NotNull @Override public String getMaxPossibleText() {
					return textPrefix + "999/999";
				}

				@Override public String getTooltipText() {
					return "Shows amount of changed lines in current change list.";
				}

				@Override public Consumer<MouseEvent> getClickConsumer() {
					return new Consumer<MouseEvent>() {
						@Override public void consume(MouseEvent mouseEvent) {
							DataContext dataContext = DataManager.getInstance().getDataContext(mouseEvent.getComponent());
							Project project = PlatformDataKeys.PROJECT.getData(dataContext);
							if (project == null) return;

							LimitedWIPProjectComponent limitedWIPProjectComponent = project.getComponent(LimitedWIPProjectComponent.class);
							limitedWIPProjectComponent.skipNotificationsUntilCommit(false);
						}
					};
				}

				@Override public float getAlignment() {
					return Component.CENTER_ALIGNMENT;
				}
			};
		}

		@NotNull @Override public String ID() {
			return "LimitedWIP_" + this.getClass().getSimpleName();
		}
	}

	private static class AutoRevertStatusBarWidget implements StatusBarWidget {
		private static final String timeTillRevertText = "Auto-revert in ";
		private static final String startedText = "Auto-revert started";
		private static final String stoppedText = "Auto-revert stopped";

		private String text = "";

		@Override public void install(@NotNull StatusBar statusBar) {
		}

		@Override public void dispose() {
		}

		public void showTime(String timeLeft) {
			text = timeTillRevertText + timeLeft;
		}

		public void showStartedText() {
			text = startedText;
		}

		public void showStoppedText() {
			text = stoppedText;
		}

		@Override public WidgetPresentation getPresentation(@NotNull PlatformType type) {
			return new StatusBarWidget.TextPresentation() {
				@NotNull @Override public String getText() {
					return text;
				}

				@NotNull @Override public String getMaxPossibleText() {
					return timeTillRevertText + "99:99";
				}

				@Override public String getTooltipText() {
					return "Click to start/stop auto-revert";
				}

				@Override public Consumer<MouseEvent> getClickConsumer() {
					return new Consumer<MouseEvent>() {
						@Override public void consume(MouseEvent mouseEvent) {
							DataContext dataContext = DataManager.getInstance().getDataContext(mouseEvent.getComponent());
							Project project = PlatformDataKeys.PROJECT.getData(dataContext);
							if (project == null) return;

							LimitedWIPProjectComponent limitedWIPProjectComponent = project.getComponent(LimitedWIPProjectComponent.class);
							if (limitedWIPProjectComponent.isAutoRevertStarted()) {
								limitedWIPProjectComponent.stopAutoRevert();
							} else {
								limitedWIPProjectComponent.startAutoRevert();
							}
						}
					};
				}

				@Override public float getAlignment() {
					return Component.CENTER_ALIGNMENT;
				}
			};
		}

		@NotNull @Override public String ID() {
			return "LimitedWIP_" + this.getClass().getSimpleName();
		}
	}
}
