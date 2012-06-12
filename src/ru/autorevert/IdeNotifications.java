package ru.autorevert;

import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import org.jetbrains.annotations.NotNull;

/**
* User: dima
* Date: 12/06/2012
*/
public class IdeNotifications {
	public void onAutoRevertStarted() {
		StatusBarWidget statusBarWidget = new StatusBarWidget() {
			@NotNull @Override public String ID() {
				return "myStatusBarWidget";
			}

			@Override public WidgetPresentation getPresentation(@NotNull PlatformType type) {
				return null;
			}

			@Override public void install(@NotNull StatusBar statusBar) {
			}

			@Override public void dispose() {
			}
		};

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
}
