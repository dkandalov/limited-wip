package ru.autorevert;

import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
* User: dima
* Date: 12/06/2012
*/
public class IdeNotifications {

	private final StatusBarWidget statusBarWidget = new MyStatusBarWidget();

	public void onAutoRevertStarted() {
		// TODO implement
	}

	public void onAutoRevertStopped() {
		// TODO implement

	}

	public void onTimerReset() {
		// TODO implement

	}

	public void onTimer() {
		// TODO implement

	}

	private static class MyStatusBarWidget implements StatusBarWidget {
		public StatusBar statusBar;

		@NotNull @Override public String ID() {
			return "myStatusBarWidget";
		}

		@Override public WidgetPresentation getPresentation(@NotNull PlatformType type) {
			return new StatusBarWidget.TextPresentation() {
				@NotNull @Override public String getText() {
					return "Auto-revert in 12:48";
				}

				@NotNull @Override public String getMaxPossibleText() {
					return "Auto-revert in 12:48";
				}

				@Override public Consumer<MouseEvent> getClickConsumer() {
					return new Consumer<MouseEvent>() {
						@Override public void consume(MouseEvent mouseEvent) {
							// TODO
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

		@Override public void install(@NotNull StatusBar statusBar) {
			this.statusBar = statusBar;
		}

		@Override public void dispose() {
			statusBar = null;
		}
	}
}
