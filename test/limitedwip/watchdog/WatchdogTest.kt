package limitedwip.watchdog

import limitedwip.watchdog.Watchdog.Settings
import limitedwip.watchdog.components.IdeAdapter
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.internal.matchers.InstanceOf
import org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress
import org.mockito.Mockito.`when` as whenCalled

class WatchdogTest {

    private val ideAdapter = mock(IdeAdapter::class.java)
    private val settings = Settings(true, maxLinesInChange, notificationIntervalInSeconds, true)
    private val watchdog = Watchdog(ideAdapter).init(settings)

    private var seconds: Int = 0


    @Test fun `does not send notification when change size is below threshold`() {
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(10))

        watchdog.onTimer(next())

        verify(ideAdapter, times(0)).onChangeSizeTooBig(anyChangeSize(), anyInt())
    }

    @Test fun `sends notification when change size is above threshold`() {
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.onTimer(next())

        verify(ideAdapter).onChangeSizeTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `sends change size notification only on one of several updates`() {
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.onTimer(next()) // send notification
        watchdog.onTimer(next())
        watchdog.onTimer(next()) // send notification
        watchdog.onTimer(next())

        verify(ideAdapter, times(2)).onChangeSizeTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `sends change size notification after settings change`() {
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))
        val inOrder = inOrder(ideAdapter)

        watchdog.onTimer(next())
        inOrder.verify(ideAdapter).onChangeSizeTooBig(ChangeSize(200), maxLinesInChange)

        watchdog.onSettings(settingsWithChangeSizeThreshold(150))
        watchdog.onTimer(next())
        inOrder.verify(ideAdapter).onChangeSizeTooBig(ChangeSize(200), 150)
    }

    @Test fun `does not send notification when disabled`() {
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.onSettings(watchdogDisabledSettings())
        watchdog.onTimer(next())
        watchdog.onTimer(next())

        verify(ideAdapter, times(2)).onSettingsUpdate(anySettings())
        verifyNoMoreInteractions(ideAdapter)
    }

    @Test fun `can skip notifications util next commit`() {
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.skipNotificationsUntilCommit(true)
        watchdog.onTimer(next())
        watchdog.onTimer(next())
        watchdog.onCommit()
        watchdog.onTimer(next())

        verify(ideAdapter).onChangeSizeTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `sends change size update`() {
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))
        watchdog.onTimer(next())
        verify(ideAdapter).showCurrentChangeListSize(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `still sends change size update when notifications are skipped till next commit`() {
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.skipNotificationsUntilCommit(true)
        watchdog.onTimer(next())

        verify(ideAdapter).showCurrentChangeListSize(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `does not send change size update when disabled`() {
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.onSettings(watchdogDisabledSettings())
        watchdog.onTimer(next())

        verify(ideAdapter, times(2)).onSettingsUpdate(anySettings())
        verifyNoMoreInteractions(ideAdapter)
    }

    @Test fun `close notification when change size is back within limit`() {
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))
        watchdog.onTimer(next())
        whenCalled(ideAdapter.currentChangeListSizeInLines()).thenReturn(ChangeSize(0))
        watchdog.onTimer(next())
        watchdog.onTimer(next())

        verify(ideAdapter, times(1)).onChangeSizeTooBig(ChangeSize(200), maxLinesInChange)
        verify(ideAdapter, times(2)).onChangeSizeWithinLimit()
    }

    private fun next(): Int = ++seconds

    private fun anySettings(): Watchdog.Settings {
        val type = Watchdog.Settings::class.java
        mockingProgress().argumentMatcherStorage.reportMatcher(InstanceOf.VarArgAware(type, "<any " + type.canonicalName + ">"))
        return Watchdog.Settings(false, 0, 0, false)
    }
    private fun anyChangeSize(): ChangeSize {
        val type = ChangeSize::class.java
        mockingProgress().argumentMatcherStorage.reportMatcher(InstanceOf.VarArgAware(type, "<any " + type.canonicalName + ">"))
        return ChangeSize(0, false)
    }

    companion object {
        private const val maxLinesInChange = 100
        private const val notificationIntervalInSeconds = 2

        private fun watchdogDisabledSettings() = Settings(false, 150, 2, true)

        private fun settingsWithChangeSizeThreshold(maxLinesInChange: Int) =
            Settings(true, maxLinesInChange, 2, true)
    }
}