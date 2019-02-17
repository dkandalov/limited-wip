package limitedwip.watchdog

import limitedwip.expect
import limitedwip.expectNoMoreInteractions
import limitedwip.shouldEqual
import limitedwip.watchdog.Watchdog.Settings
import limitedwip.watchdog.components.Ide
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.internal.matchers.InstanceOf
import org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress
import org.mockito.Mockito.`when` as whenCalled

class WatchdogTests {
    private val maxLinesInChange = 100
    private val settings = Settings(
        enabled = true,
        maxLinesInChange = maxLinesInChange,
        notificationIntervalInSeconds = 2,
        showRemainingChangesInToolbar = true,
        noCommitsAboveThreshold = false,
        exclusions = "some/excluded/path"
    )
    private val disabledSettings = settings.copy(enabled = false)
    private var timer: Int = 0

    private val ide = mock(Ide::class.java)
    private val watchdog = Watchdog(ide, settings)

    @Test fun `don't send notification when change size is below threshold`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(10))

        watchdog.onTimer(next())

        ide.expect(never()).showNotificationThatChangeSizeIsTooBig(anyChangeSize(), anyInt())
    }

    @Test fun `don't send notification when change size has excluded paths which make it below threshold`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSizesWithPath(listOf(
            Pair("some/path", ChangeSize(90)),
            Pair("some/excluded/path", ChangeSize(90))
        )))

        watchdog.onTimer(next())

        ide.expect(never()).showNotificationThatChangeSizeIsTooBig(anyChangeSize(), anyInt())
    }

    @Test fun `send notification when change size is above threshold`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))

        watchdog.onTimer(next())

        ide.expect().showNotificationThatChangeSizeIsTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `send change size notification only on one of several updates`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))

        watchdog.onTimer(next()) // send notification
        watchdog.onTimer(next())
        watchdog.onTimer(next()) // send notification
        watchdog.onTimer(next())

        ide.expect(times(2)).showNotificationThatChangeSizeIsTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `send change size notification after settings change`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))
        val inOrder = inOrder(ide)

        watchdog.onTimer(next())
        ide.expect(inOrder).showNotificationThatChangeSizeIsTooBig(ChangeSize(200), maxLinesInChange)

        watchdog.onSettingsUpdate(settings.copy(maxLinesInChange = 150))
        ide.expect(inOrder).showCurrentChangeListSize(ChangeSize(200), 150)

        watchdog.onTimer(next())
        ide.expect(inOrder).showNotificationThatChangeSizeIsTooBig(ChangeSize(200), 150)
    }

    @Test fun `don't send notification when disabled`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))

        watchdog.onSettingsUpdate(disabledSettings)
        watchdog.onTimer(next())
        watchdog.onTimer(next())

        ide.expect(times(2)).onSettingsUpdate(anySettings())
        ide.expectNoMoreInteractions()
    }

    @Test fun `skip notifications util next commit`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))

        watchdog.onSkipNotificationsUntilCommit()
        watchdog.onTimer(next())
        watchdog.onTimer(next())
        watchdog.onSuccessfulCommit()
        watchdog.onTimer(next())

        ide.expect().showNotificationThatChangeSizeIsTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `send change size update`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))
        watchdog.onTimer(next())
        ide.expect().showCurrentChangeListSize(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `still send change size update when notifications are skipped till next commit`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))

        watchdog.onSkipNotificationsUntilCommit()
        watchdog.onTimer(next())

        ide.expect().showCurrentChangeListSize(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `don't send change size update when disabled`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))

        watchdog.onSettingsUpdate(disabledSettings)
        watchdog.onTimer(next())

        ide.expect(times(2)).onSettingsUpdate(anySettings())
        ide.expectNoMoreInteractions()
    }

    @Test fun `close notification when change size is back within limit`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))
        watchdog.onTimer(next())
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(0))
        watchdog.onTimer(next())
        watchdog.onTimer(next())

        ide.expect(times(1)).showNotificationThatChangeSizeIsTooBig(ChangeSize(200), maxLinesInChange)
        ide.expect(times(2)).hideNotificationThatChangeSizeIsTooBig()
    }

    @Test fun `don't allow commits above threshold`() {
        watchdog.onSettingsUpdate(settings.copy(noCommitsAboveThreshold = true))

        watchdog.isCommitAllowed(someChangesWithSize(200)) shouldEqual false
        ide.expect().notifyThatCommitWasCancelled()
    }

    @Test fun `allow one commit above threshold when forced`() {
        watchdog.onSettingsUpdate(settings.copy(noCommitsAboveThreshold = true))

        watchdog.isCommitAllowed(someChangesWithSize(200)) shouldEqual false
        ide.expect().notifyThatCommitWasCancelled()

        watchdog.onForceCommit()
        watchdog.isCommitAllowed(someChangesWithSize(200)) shouldEqual true
    }

    private fun next(): Int = ++timer

    private fun someChangesWithSize(size: Int) =
        ChangeSizesWithPath(listOf(Pair("", ChangeSize(size))))

    private fun anySettings(): Watchdog.Settings {
        val type = Watchdog.Settings::class.java
        mockingProgress().argumentMatcherStorage.reportMatcher(InstanceOf.VarArgAware(type, "<any ${type.canonicalName}>"))
        return Watchdog.Settings(false, 0, 0, false, false, "")
    }

    private fun anyChangeSize(): ChangeSize {
        val type = ChangeSize::class.java
        mockingProgress().argumentMatcherStorage.reportMatcher(InstanceOf.VarArgAware(type, "<any ${type.canonicalName}>"))
        return ChangeSize(0, false)
    }
}