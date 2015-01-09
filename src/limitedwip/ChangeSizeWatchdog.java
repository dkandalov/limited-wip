package limitedwip;

public class ChangeSizeWatchdog {
    private final IdeNotifications ideNotifications;
    private final Settings settings;

    private int lastNotificationTime = -1;

    public ChangeSizeWatchdog(IdeNotifications ideNotifications, Settings settings) {
        this.ideNotifications = ideNotifications;
        this.settings = settings;
    }

    public synchronized void onChangeSizeUpdate(int changeListSizeInLines, int seconds) {
        boolean exceededThreshold = changeListSizeInLines > settings.maxLinesInChange;
        boolean timeToNotify =
                lastNotificationTime == -1 ||
                (seconds - lastNotificationTime) >= settings.notificationIntervalInSeconds;

        if (exceededThreshold && timeToNotify) {
            ideNotifications.onChangeExceededThreshold(
                    changeListSizeInLines,
                    settings.maxLinesInChange
            );
            lastNotificationTime = seconds;
        }
    }

    public synchronized void onNewSettings(int maxLinesInChange, boolean disableCommitsAboveThreshold) {
        // TODO implement

    }

    public static class Settings {
        public final int maxLinesInChange;
        public final boolean disableCommitsAboveThreshold;
        public final int notificationIntervalInSeconds;

        public Settings(boolean disableCommitsAboveThreshold, int maxLinesInChange, int notificationIntervalInSeconds) {
            this.disableCommitsAboveThreshold = disableCommitsAboveThreshold;
            this.maxLinesInChange = maxLinesInChange;
            this.notificationIntervalInSeconds = notificationIntervalInSeconds;
        }
    }
}
