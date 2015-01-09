package limitedwip;

public class ChangeSizeWatchdog {
    private final IdeNotifications ideNotifications;

    public ChangeSizeWatchdog(IdeNotifications ideNotifications) {
        this.ideNotifications = ideNotifications;
    }

    public synchronized void onChangeSizeUpdate(int changeListSizeInLines) {
        // TODO implement

    }

    public synchronized void onNewSettings(int maxLinesInChange, boolean disableCommitsAboveThreshold) {
        // TODO implement

    }
}
