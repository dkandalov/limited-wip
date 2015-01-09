package limitedwip;

public class ChangeSizeWatchdog {
    private final IdeNotifications ideNotifications;

    public ChangeSizeWatchdog(IdeNotifications ideNotifications, Settings settings) {
        this.ideNotifications = ideNotifications;
    }

    public synchronized void onChangeSizeUpdate(int changeListSizeInLines, int seconds) {
        // TODO implement

    }

    public synchronized void onNewSettings(int maxLinesInChange, boolean disableCommitsAboveThreshold) {
        // TODO implement

    }

    public static class Settings {
        public final int maxLinesInChange;
        public final boolean disableCommitsAboveThreshold;

        public Settings(boolean disableCommitsAboveThreshold, int maxLinesInChange) {
            this.disableCommitsAboveThreshold = disableCommitsAboveThreshold;
            this.maxLinesInChange = maxLinesInChange;
        }
    }
}
