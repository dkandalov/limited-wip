package limitedwip.watchdog;

import limitedwip.watchdog.ChangeSizeWatchdog.Settings;
import limitedwip.watchdog.components.IdeAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;

import static org.mockito.Mockito.*;

public class ChangeSizeWatchdogTest {
    private static final int maxLinesInChange = 100;
    private static final int notificationIntervalInSeconds = 2;

    private final IdeAdapter ideAdapter = mock(IdeAdapter.class);
    private final Settings settings = new Settings(true, maxLinesInChange, notificationIntervalInSeconds, true);
    private final ChangeSizeWatchdog watchdog = new ChangeSizeWatchdog(ideAdapter).init(settings);

    private int secondsSinceStart;


    @Test public void doesNotSendNotification_WhenChangeSizeIsBelowThreshold() {
        when(ideAdapter.currentChangeListSizeInLines()).thenReturn(new ChangeSize(10));

        watchdog.onTimer(next());

        verify(ideAdapter, times(0)).onChangeSizeTooBig(Matchers.<ChangeSize>any(), anyInt());
    }

    @Test public void sendsNotification_WhenChangeSizeIsAboveThreshold() {
        when(ideAdapter.currentChangeListSizeInLines()).thenReturn(new ChangeSize(200));

        watchdog.onTimer(next());

        verify(ideAdapter).onChangeSizeTooBig(new ChangeSize(200), maxLinesInChange);
    }

    @Test public void sendsChangeSizeNotification_OnlyOnOneOfSeveralUpdates() {
        when(ideAdapter.currentChangeListSizeInLines()).thenReturn(new ChangeSize(200));

        watchdog.onTimer(next()); // send notification
        watchdog.onTimer(next());
        watchdog.onTimer(next()); // send notification
        watchdog.onTimer(next());

        verify(ideAdapter, times(2)).onChangeSizeTooBig(new ChangeSize(200), maxLinesInChange);
    }

    @Test public void sendsChangeSizeNotification_AfterSettingsChange() {
        when(ideAdapter.currentChangeListSizeInLines()).thenReturn(new ChangeSize(200));
        InOrder inOrder = inOrder(ideAdapter);

        watchdog.onTimer(next());
        inOrder.verify(ideAdapter).onChangeSizeTooBig(new ChangeSize(200), maxLinesInChange);

        watchdog.onSettings(settingsWithChangeSizeThreshold(150));
        watchdog.onTimer(next());
        inOrder.verify(ideAdapter).onChangeSizeTooBig(new ChangeSize(200), 150);
    }

    @Test public void doesNotSendNotification_WhenDisabled() {
        when(ideAdapter.currentChangeListSizeInLines()).thenReturn(new ChangeSize(200));

        watchdog.onSettings(watchdogDisabledSettings());
        watchdog.onTimer(next());
        watchdog.onTimer(next());

	    verify(ideAdapter, times(2)).onSettingsUpdate(Matchers.<Settings>anyObject());
	    verifyNoMoreInteractions(ideAdapter);
    }

    @Test public void canSkipNotificationsUtilNextCommit() {
        when(ideAdapter.currentChangeListSizeInLines()).thenReturn(new ChangeSize(200));

        watchdog.skipNotificationsUntilCommit(true);
        watchdog.onTimer(next());
        watchdog.onTimer(next());
        watchdog.onCommit();
        watchdog.onTimer(next());

        verify(ideAdapter).onChangeSizeTooBig(new ChangeSize(200), maxLinesInChange);
    }

    @Test public void sendsChangeSizeUpdate() {
        when(ideAdapter.currentChangeListSizeInLines()).thenReturn(new ChangeSize(200));
        watchdog.onTimer(next());
        verify(ideAdapter).currentChangeListSize(new ChangeSize(200), maxLinesInChange);
    }

    @Test public void sendsChangeSizeUpdate_WhenSkipNotificationUntilNextCommit() {
        when(ideAdapter.currentChangeListSizeInLines()).thenReturn(new ChangeSize(200));

        watchdog.skipNotificationsUntilCommit(true);
        watchdog.onTimer(next());

        verify(ideAdapter).currentChangeListSize(new ChangeSize(200), maxLinesInChange);
    }

    @Test public void doesNotSendChangeSizeUpdate_WhenDisabled() {
        when(ideAdapter.currentChangeListSizeInLines()).thenReturn(new ChangeSize(200));

        watchdog.onSettings(watchdogDisabledSettings());
        watchdog.onTimer(next());

	    verify(ideAdapter, times(2)).onSettingsUpdate(Matchers.<Settings>anyObject());
        verifyNoMoreInteractions(ideAdapter);
    }

    @Before public void setUp() throws Exception {
        secondsSinceStart = 0;
    }

    private int next() {
        return ++secondsSinceStart;
    }

    private static Settings watchdogDisabledSettings() {
        return new Settings(false, 150, 2, true);
    }

    private static Settings settingsWithChangeSizeThreshold(int maxLinesInChange) {
        return new Settings(true, maxLinesInChange, 2, true);
    }
}