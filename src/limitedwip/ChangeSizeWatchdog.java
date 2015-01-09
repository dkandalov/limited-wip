package limitedwip;

public class ChangeSizeWatchdog {
    private final IdeNotifications ideNotifications;

    private Settings settings;
    private int lastNotificationTime = -1;


    public ChangeSizeWatchdog(IdeNotifications ideNotifications, Settings settings) {
        this.ideNotifications = ideNotifications;
        this.settings = settings;
        onSettings(settings);
    }

    public synchronized void onChangeSizeUpdate(int changeListSizeInLines, int seconds) {
        if (!settings.enabled) return;

        boolean exceededThreshold = changeListSizeInLines > settings.maxLinesInChange;
        boolean timeToNotify =
                lastNotificationTime == -1 ||
                (seconds - lastNotificationTime) >= settings.notificationIntervalInSeconds;

        if (exceededThreshold && timeToNotify) {
            ideNotifications.onChangeSizeTooBig(
                    changeListSizeInLines,
                    settings.maxLinesInChange
            );
            lastNotificationTime = seconds;
        }
    }

    public synchronized void onSettings(Settings settings) {
        lastNotificationTime = -1;
        this.settings = settings;
    }

    public synchronized void skipNotificationsUntilCommit() {
        // TODO
    }


    public static class Settings {
        public final boolean enabled;
        public final int maxLinesInChange;
        public final int notificationIntervalInSeconds;

        public Settings(boolean enabled, int maxLinesInChange, int notificationIntervalInSeconds) {
            this.enabled = enabled;
            this.maxLinesInChange = maxLinesInChange;
            this.notificationIntervalInSeconds = notificationIntervalInSeconds;
        }
    }
}
