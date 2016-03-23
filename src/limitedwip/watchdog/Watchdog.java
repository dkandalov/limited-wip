package limitedwip.watchdog;

import limitedwip.watchdog.components.IdeAdapter;

public class Watchdog {
    private final IdeAdapter ideAdapter;

    private Settings settings;
    private int lastNotificationTime = -1;
    private boolean skipNotificationsUtilCommit = false;


    public Watchdog(IdeAdapter ideAdapter) {
        this.ideAdapter = ideAdapter;
    }

	public Watchdog init(Settings settings) {
		onSettings(settings);
		return this;
	}

	public void onTimer(int seconds) {
        if (!settings.enabled) return;

        ChangeSize changeListSizeInLines = ideAdapter.currentChangeListSizeInLines();

        if (!skipNotificationsUtilCommit) {
            boolean exceededThreshold = changeListSizeInLines.value > settings.maxLinesInChange;
            boolean timeToNotify =
                    lastNotificationTime == -1 ||
                            (seconds - lastNotificationTime) >= settings.notificationIntervalInSeconds;

            if (exceededThreshold && timeToNotify) {
                ideAdapter.onChangeSizeTooBig(changeListSizeInLines, settings.maxLinesInChange);
                lastNotificationTime = seconds;
            }
        }

        ideAdapter.currentChangeListSize(changeListSizeInLines, settings.maxLinesInChange);
    }

    public void onSettings(Settings settings) {
	    ideAdapter.onSettingsUpdate(settings);
	    lastNotificationTime = -1;
        this.settings = settings;
    }

    public void onCommit() {
        skipNotificationsUtilCommit = false;
    }

    public void skipNotificationsUntilCommit(boolean value) {
        skipNotificationsUtilCommit = value;
        lastNotificationTime = -1;
	    ideAdapter.onSkipNotificationUntilCommit(value);
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
