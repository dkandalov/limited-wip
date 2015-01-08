package limitedwip;

public class ChangeSizeWatchdog {
    private final IdeNotifications ideNotifications;

    public ChangeSizeWatchdog(IdeNotifications ideNotifications) {
        this.ideNotifications = ideNotifications;
    }

    public void onChangeSizeUpdate(int changeListSizeInLines) {
        // TODO implement

    }

    public void onNewSettings(int maxLinesInChange, boolean disableCommitsAboveThreshold) {
        // TODO implement

    }
}
