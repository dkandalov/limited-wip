package limitedwip.watchdog;

import limitedwip.watchdog.components.IdeActions;
import limitedwip.watchdog.components.IdeNotifications;
import limitedwip.watchdog.components.ChangeSize;

public class ChangeSizeWatchdog {
    private final IdeNotifications ideNotifications;

    private final IdeActions ideActions;
    private Settings settings;
    private int lastNotificationTime = -1;
    private boolean skipNotificationsUtilCommit = false;


    public ChangeSizeWatchdog(IdeNotifications ideNotifications, IdeActions ideActions, Settings settings) {
        this.ideNotifications = ideNotifications;
        this.ideActions = ideActions;
        this.settings = settings;
        onSettings(settings);
    }

    public synchronized void onTimer(int seconds) {
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

    public synchronized void onSettings(Settings settings) {
        lastNotificationTime = -1;
        this.settings = settings;
    }

    public synchronized void onCommit() {
        skipNotificationsUtilCommit = false;
    }

    public synchronized void skipNotificationsUntilCommit(boolean value) {
        skipNotificationsUtilCommit = value;
        lastNotificationTime = -1;
    }

    public synchronized boolean toggleSkipNotificationsUntilCommit() {
        skipNotificationsUntilCommit(!skipNotificationsUtilCommit);
        return skipNotificationsUtilCommit;
    }


    public static class Settings {
	    private final boolean enabled;
	    private final int maxLinesInChange;
	    private final int notificationIntervalInSeconds;

        public Settings(boolean enabled, int maxLinesInChange, int notificationIntervalInSeconds) {
            this.enabled = enabled;
            this.maxLinesInChange = maxLinesInChange;
            this.notificationIntervalInSeconds = notificationIntervalInSeconds;
        }
    }
}
