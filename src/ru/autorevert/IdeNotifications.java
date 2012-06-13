package ru.autorevert;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
* User: dima
* Date: 12/06/2012
*/
public class IdeNotifications {

	private final MyStatusBarWidget widget = new MyStatusBarWidget();
	private final Project project;

	public IdeNotifications(Project project) {
		this.project = project;

		widget.showThatStopped();

		StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
		if (statusBar == null) return;

		statusBar.removeWidget(widget.ID());
		statusBar.addWidget(widget);
		statusBar.updateWidget(widget.ID());
	}

	public void onAutoRevertStarted(int timeEventsTillRevert) {
		onTimer(timeEventsTillRevert);
	}

	public void onAutoRevertStopped() {
		widget.showThatStopped();
	}

	public void onTimer(int secondsLeft) {
		widget.showTime(formatTime(secondsLeft));

		StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
		if (statusBar == null) return;
		statusBar.updateWidget(widget.ID());
	}

	private static String formatTime(int seconds) {
		int min = seconds / 60;
		int sec = seconds % 60;
		return String.format("%02d", min) + ":" + String.format("%02d", sec);
	}

	private static class MyStatusBarWidget implements StatusBarWidget {
		private static final String TIME_LEFT_PREFIX_TEXT = "Auto-revert in ";
		private static final String STOPPED_TEXT = "Auto-revert in ";

		public StatusBar statusBar;
		private String text = "";

		@Override public void install(@NotNull StatusBar statusBar) {
			this.statusBar = statusBar;
		}

		@Override public void dispose() {
			statusBar = null;
		}

		public void showTime(String timeLeft) {
			text = TIME_LEFT_PREFIX_TEXT + timeLeft;
		}

		public void showThatStopped() {
			text = STOPPED_TEXT;
		}

		@Override public WidgetPresentation getPresentation(@NotNull PlatformType type) {
			return new StatusBarWidget.TextPresentation() {
				@NotNull @Override public String getText() {
					return text;
				}

				@NotNull @Override public String getMaxPossibleText() {
					return TIME_LEFT_PREFIX_TEXT + "99:99";
				}

				@Override public Consumer<MouseEvent> getClickConsumer() {
					return new Consumer<MouseEvent>() {
						@Override public void consume(MouseEvent mouseEvent) {
							// TODO ?
						}
					};
				}

				@Override public float getAlignment() {
					return Component.CENTER_ALIGNMENT;
				}

				@Override public String getTooltipText() {
					return null;
				}
			};
		}

		@NotNull @Override public String ID() {
			return "AutoRevert_StatusBarWidget";
		}
	}
}
