package limitedwip.watchdog;

import limitedwip.watchdog.components.ChangeSize;
import limitedwip.watchdog.components.IdeActions;
import limitedwip.watchdog.components.IdeNotifications;

public class ChangeSizeWatchdog {
    private final IdeNotifications ideNotifications;

    private final IdeActions ideActions;
    private Settings settings;
    private int lastNotificationTime = -1;
    private boolean skipNotificationsUtilCommit = false;


    public ChangeSizeWatchdog(IdeNotifications ideNotifications, IdeActions ideActions) {
        this.ideNotifications = ideNotifications;
        this.ideActions = ideActions;
    }

	public ChangeSizeWatchdog init(Settings settings) {
		onSettings(settings);
		return this;
	}

	public void onTimer(int seconds) {
        if (!settings.enabled) return;

        ChangeSize changeListSizeInLines = ideActions.currentChangeListSizeInLines();

        if (!skipNotificationsUtilCommit) {
            boolean exceededThreshold = changeListSizeInLines.value > settings.maxLinesInChange;
            boolean timeToNotify =
                    lastNotificationTime == -1 ||
                            (seconds - lastNotificationTime) >= settings.notificationIntervalInSeconds;

            if (exceededThreshold && timeToNotify) {
                ideNotifications.onChangeSizeTooBig(changeListSizeInLines, settings.maxLinesInChange);
                lastNotificationTime = seconds;
            }
        }

        ideNotifications.currentChangeListSize(changeListSizeInLines, settings.maxLinesInChange);
    }

    public void onSettings(Settings settings) {
	    ideNotifications.onSettingsUpdate(settings);
	    lastNotificationTime = -1;
        this.settings = settings;
    }

    public void onCommit() {
        skipNotificationsUtilCommit = false;
    }

    public void skipNotificationsUntilCommit(boolean value) {
        skipNotificationsUtilCommit = value;
        lastNotificationTime = -1;
	    ideNotifications.onSkipNotificationUntilCommit(value);
    }

    public void toggleSkipNotificationsUntilCommit() {
        skipNotificationsUntilCommit(!skipNotificationsUtilCommit);
    }


    public static class Settings {
	    public final boolean enabled;
	    public final int maxLinesInChange;
	    public final int notificationIntervalInSeconds;
	    public final boolean showRemainingChangesInToolbar;

        public Settings(boolean enabled, int maxLinesInChange, int notificationIntervalInSeconds, boolean showRemainingChangesInToolbar) {
            this.enabled = enabled;
            this.maxLinesInChange = maxLinesInChange;
            this.notificationIntervalInSeconds = notificationIntervalInSeconds;
	        this.showRemainingChangesInToolbar = showRemainingChangesInToolbar;
        }
    }
}
