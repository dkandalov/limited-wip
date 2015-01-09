package limitedwip;

public class ChangeSizeWatchdog {
    private final IdeNotifications ideNotifications;
    private final Settings settings;

    public ChangeSizeWatchdog(IdeNotifications ideNotifications, Settings settings) {
        this.ideNotifications = ideNotifications;
        this.settings = settings;
    }

    public synchronized void onChangeSizeUpdate(int changeListSizeInLines, int seconds) {
        if (changeListSizeInLines > settings.maxLinesInChange) {
            ideNotifications.onChangeExceededThreshold(
                    changeListSizeInLines,
                    settings.maxLinesInChange
            );
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
