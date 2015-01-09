package limitedwip;

import limitedwip.ChangeSizeWatchdog.Settings;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ChangeSizeWatchdogTest {
    private int secondsSinceStart;

    @Test public void aaa() {
        IdeNotifications ideNotifications = mock(IdeNotifications.class);
        ChangeSizeWatchdog watchdog = new ChangeSizeWatchdog(ideNotifications, new Settings(
                false,
                100
        ));

        watchdog.onChangeSizeUpdate(10, next());

        verifyZeroInteractions(ideNotifications);
    }

    @Before public void setUp() throws Exception {
        secondsSinceStart = 0;
    }

    private int next() {
        return ++secondsSinceStart;
    }
}