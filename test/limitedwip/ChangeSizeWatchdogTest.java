package limitedwip;

import limitedwip.ChangeSizeWatchdog.Settings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

public class ChangeSizeWatchdogTest {
    private static final int maxLinesInChange = 100;
    private static final int notificationIntervalInSeconds = 2;

    private final IdeNotifications ideNotifications = mock(IdeNotifications.class);
    private final Settings settings = new Settings(true, maxLinesInChange, notificationIntervalInSeconds);
    private final ChangeSizeWatchdog watchdog = new ChangeSizeWatchdog(ideNotifications, settings);

    private int secondsSinceStart;


    @Test public void doesNotSendNotification_WhenChangeSizeIsBelowThreshold() {
        watchdog.onChangeSizeUpdate(10, next());

        verify(ideNotifications, times(0)).onChangeSizeTooBig(anyInt(), anyInt());
    }

    @Test public void sendsNotification_WhenChangeSizeIsAboveThreshold() {
        watchdog.onChangeSizeUpdate(200, next());

        verify(ideNotifications).onChangeSizeTooBig(200, maxLinesInChange);
    }

    @Test public void sendsChangeSizeNotification_OnlyOnOneOfSeveralUpdates() {
        watchdog.onChangeSizeUpdate(200, next()); // send notification
        watchdog.onChangeSizeUpdate(200, next());
        watchdog.onChangeSizeUpdate(200, next()); // send notification
        watchdog.onChangeSizeUpdate(200, next());

        verify(ideNotifications, times(2)).onChangeSizeTooBig(200, maxLinesInChange);
    }

    @Test public void sendsChangeSizeNotification_AfterSettingsChange() {
        InOrder inOrder = inOrder(ideNotifications);

        watchdog.onChangeSizeUpdate(200, next());
        inOrder.verify(ideNotifications).onChangeSizeTooBig(200, maxLinesInChange);

        watchdog.onSettings(settingsWithChangeSizeThreshold(150));
        watchdog.onChangeSizeUpdate(200, next());
        inOrder.verify(ideNotifications).onChangeSizeTooBig(200, 150);
    }

    @Test public void doesNotSendNotification_WhenDisabled() {
        watchdog.onSettings(watchdogDisabledSettings());
        watchdog.onChangeSizeUpdate(200, next());
        watchdog.onChangeSizeUpdate(200, next());

        verifyZeroInteractions(ideNotifications);
    }

    @Test public void canSkipNotificationsUtilNextCommit() {
        watchdog.skipNotificationsUntilCommit(true);
        watchdog.onChangeSizeUpdate(200, next());
        watchdog.onChangeSizeUpdate(200, next());
        watchdog.onCommit();
        watchdog.onChangeSizeUpdate(200, next());

        verify(ideNotifications).onChangeSizeTooBig(200, maxLinesInChange);
    }

    @Before public void setUp() throws Exception {
        secondsSinceStart = 0;
    }

    private int next() {
        return ++secondsSinceStart;
    }

    private static Settings watchdogDisabledSettings() {
        return new Settings(false, 150, 2);
    }

    private static Settings settingsWithChangeSizeThreshold(int maxLinesInChange) {
        return new Settings(true, maxLinesInChange, 2);
    }
}