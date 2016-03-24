package limitedwip.watchdog;

import limitedwip.watchdog.components.IdeAdapter;

public class Watchdog {
	private static final int undefined = -1;

	private final IdeAdapter ideAdapter;
    private Settings settings;
    private int lastNotificationTime = undefined;
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

        ChangeSize changeSize = ideAdapter.currentChangeListSizeInLines();
		boolean exceededThreshold = changeSize.value > settings.maxLinesInChange;
		boolean timeToNotify =
		        lastNotificationTime == undefined ||
		        (seconds - lastNotificationTime) >= settings.notificationIntervalInSeconds;

		if (timeToNotify && exceededThreshold && !skipNotificationsUtilCommit) {
			ideAdapter.onChangeSizeTooBig(changeSize, settings.maxLinesInChange);
			lastNotificationTime = seconds;
		}
		if (!exceededThreshold){
			ideAdapter.onChangeSizeWithinLimit();
		}

		ideAdapter.showCurrentChangeListSize(changeSize, settings.maxLinesInChange);
	}

    public void onSettings(Settings settings) {
	    ideAdapter.onSettingsUpdate(settings);
	    lastNotificationTime = undefined;
        this.settings = settings;
    }

    public void onCommit() {
	    // This is a workaround to suppress notifications sent while commit dialog is open.
	    ideAdapter.onChangeSizeWithinLimit();

	    skipNotificationsUtilCommit = false;
    }

    public void skipNotificationsUntilCommit(boolean value) {
        skipNotificationsUtilCommit = value;
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
