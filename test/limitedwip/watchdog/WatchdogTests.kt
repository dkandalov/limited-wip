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

data class Fixture(
    val maxLinesInChange: Int = 100,
    val settings: Settings = Settings(
        enabled = true,
        maxLinesInChange = maxLinesInChange,
        notificationIntervalInSeconds = 2,
        showRemainingChangesInToolbar = true,
        noCommitsAboveThreshold = true,
        exclusions = setOf(
            PathMatcher.parse("some/excluded/path"),
            PathMatcher.parse("another/excluded/path")
        )
    ),
    val ide: Ide = mock(Ide::class.java),
    val watchdog: Watchdog = Watchdog(ide, settings)
) {
    init {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))
    }

    fun someChangesWithSize(size: Int) =
        ChangeSizesWithPath(listOf(Pair("", ChangeSize(size))))

    fun anySettings(): Watchdog.Settings {
        val type = Watchdog.Settings::class.java
        mockingProgress().argumentMatcherStorage.reportMatcher(InstanceOf.VarArgAware(type, "<any ${type.canonicalName}>"))
        return Watchdog.Settings(false, 0, 0, false, false, emptySet())
    }

    fun anyChangeSize(): ChangeSize {
        val type = ChangeSize::class.java
        mockingProgress().argumentMatcherStorage.reportMatcher(InstanceOf.VarArgAware(type, "<any ${type.canonicalName}>"))
        return ChangeSize(0, false)
    }
}

class WatchdogTests {
    @Test fun `show in UI current change size`() = Fixture().run {
        watchdog.onTimer()
        ide.expect().showCurrentChangeListSize(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `send notification when change size is above threshold`() = Fixture().run {
        watchdog.onTimer() // send notification
        watchdog.onTimer()
        watchdog.onTimer() // send notification
        watchdog.onTimer()

        ide.expect(times(2)).showNotificationThatChangeSizeIsTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `don't send notification when change size is below threshold`() = Fixture().run {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(10))
        watchdog.onTimer()
        ide.expect(never()).showNotificationThatChangeSizeIsTooBig(anyChangeSize(), anyInt())
    }

    @Test fun `don't send notification when change size has excluded paths which make it below threshold`() = Fixture().run {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSizesWithPath(listOf(
            Pair("/some/path", ChangeSize(90)),
            Pair("/some/excluded/path", ChangeSize(200)),
            Pair("/another/excluded/path", ChangeSize(200))
        )))

        watchdog.onTimer()

        ide.expect(never()).showNotificationThatChangeSizeIsTooBig(anyChangeSize(), anyInt())
    }

    @Test fun `close notification when change size is back within limit`() = Fixture().run {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(200))
        watchdog.onTimer()
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(someChangesWithSize(0))
        watchdog.onTimer()
        watchdog.onTimer()

        ide.expect(times(1)).showNotificationThatChangeSizeIsTooBig(ChangeSize(200), maxLinesInChange)
        ide.expect(times(2)).hideNotificationThatChangeSizeIsTooBig()
    }

    @Test fun `send change size notification after settings have changed`() = Fixture().run {
        val inOrder = inOrder(ide)

        watchdog.onTimer()
        ide.expect(inOrder).showNotificationThatChangeSizeIsTooBig(ChangeSize(200), maxLinesInChange)

        watchdog.onSettingsUpdate(settings.copy(maxLinesInChange = 150))
        ide.expect(inOrder).showCurrentChangeListSize(ChangeSize(200), 150)

        watchdog.onTimer()
        ide.expect(inOrder).showNotificationThatChangeSizeIsTooBig(ChangeSize(200), 150)
    }
}

class WatchdogSkipNotificationsTests {
    @Test fun `skip notifications util next commit`() = Fixture().run {
        watchdog.onSkipNotificationsUntilCommit()
        watchdog.onTimer()
        watchdog.onTimer()
        watchdog.onSuccessfulCommit()
        watchdog.onTimer()

        ide.expect().showNotificationThatChangeSizeIsTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `show change size in UI when notifications are skipped till next commit`() = Fixture().run {
        watchdog.onSkipNotificationsUntilCommit()
        watchdog.onTimer()

        ide.expect().showCurrentChangeListSize(ChangeSize(200), maxLinesInChange)
    }
}

class WatchdogCommitTests {
    @Test fun `don't allow commits above threshold`() = Fixture().run {
        watchdog.isCommitAllowed(someChangesWithSize(200)) shouldEqual false
        ide.expect().notifyThatCommitWasCancelled()
    }

    @Test fun `allow one commit above threshold when forced`() = Fixture().run {
        watchdog.isCommitAllowed(someChangesWithSize(200)) shouldEqual false
        ide.expect().notifyThatCommitWasCancelled()

        watchdog.onForceCommit()
        watchdog.isCommitAllowed(someChangesWithSize(200)) shouldEqual true
    }
}

class WatchdogDisabledTests {
    private val Fixture.disabledSettings: Settings
        get() = settings.copy(enabled = false)

    @Test fun `when disabled, don't show change size or notifications`() = Fixture().run {
        watchdog.onSettingsUpdate(disabledSettings)
        watchdog.onTimer()
        watchdog.onTimer()

        ide.expect(times(2)).onSettingsUpdate(anySettings())
        ide.expectNoMoreInteractions()
    }
}