package limitedwip;

import limitedwip.ChangeSizeWatchdog.Settings;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class ChangeSizeWatchdogTest {
    private static final int maxLinesInChange = 100;
    private static final int notificationIntervalInSeconds = 2;

    private final IdeNotifications ideNotifications = mock(IdeNotifications.class);
    private final Settings settings = new Settings(false, maxLinesInChange, notificationIntervalInSeconds);
    private final ChangeSizeWatchdog watchdog = new ChangeSizeWatchdog(ideNotifications, settings);

    private int secondsSinceStart;


    @Test public void doesNotSendNotification_WhenChangeSizeIsBelowThreshold() {
        watchdog.onChangeSizeUpdate(10, next());

        verifyZeroInteractions(ideNotifications);
    }

    @Test public void sendsNotification_WhenChangeSizeIsAboveThreshold() {
        watchdog.onChangeSizeUpdate(200, next());

        verify(ideNotifications).onChangeExceededThreshold(200, maxLinesInChange);
    }

    @Test public void sendsChangeSizeNotification_OnlyOnOneOfSeveralUpdates() {
        watchdog.onChangeSizeUpdate(200, next());
        watchdog.onChangeSizeUpdate(200, next());

        verify(ideNotifications).onChangeExceededThreshold(200, maxLinesInChange);
    }

    @Before public void setUp() throws Exception {
        secondsSinceStart = 0;
    }

    private int next() {
        return ++secondsSinceStart;
    }
}