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
	}

	public void onAutoRevertStarted() {
		StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
		if (statusBar == null) return;

		statusBar.removeWidget(widget.ID());
		statusBar.addWidget(widget);
		statusBar.updateWidget(widget.ID());
	}

	public void onAutoRevertStopped() {
		StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
		if (statusBar == null) return;

		statusBar.removeWidget(widget.ID());
	}

	public void onTimer(int secondsPassed) {
		widget.showTime(secondsPassed);
	}

	public void onTimerReset() {
		widget.showTime(0);
	}

	private static class MyStatusBarWidget implements StatusBarWidget {
		private static final String PREFIX_TEXT = "Auto-revert in ";
		public StatusBar statusBar;

		@Override public void install(@NotNull StatusBar statusBar) {
			this.statusBar = statusBar;
		}

		@Override public void dispose() {
			statusBar = null;
		}

		public void showTime(int secondsPassed) {
			// TODO implement

		}

		@Override public WidgetPresentation getPresentation(@NotNull PlatformType type) {
			return new StatusBarWidget.TextPresentation() {
				@NotNull @Override public String getText() {
					return PREFIX_TEXT;
				}

				@NotNull @Override public String getMaxPossibleText() {
					return PREFIX_TEXT + "99:99";
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
