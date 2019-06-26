package limitedwip.autorevert

import limitedwip.autorevert.AutoRevert.Settings
import limitedwip.autorevert.components.Ide
import limitedwip.expect
import limitedwip.expectNoMoreInteractions
import org.junit.Test
import org.mockito.InOrder
import org.mockito.Mockito.*
import org.mockito.Mockito.`when` as whenCalled

private data class Fixture(
    val ide: Ide = mock(Ide::class.java),
    val inOrder: InOrder = inOrder(ide),
    val settings: Settings = Settings(
        enabled = true,
        secondsTillRevert = 1,
        notifyOnRevert = true
    ),
    val autoRevert: AutoRevert = AutoRevert(ide, settings)
) {
    init {
        whenCalled(ide.revertCurrentChangeList()).thenReturn(10)
    }

    fun run(f: Fixture.() -> Unit) = f(this)
}

class AutoRevertBasicTests {
    @Test fun `start timer when some changes are made`() = Fixture().run {
        autoRevert.onTimer(hasChanges = false)
        autoRevert.onTimer(hasChanges = false)
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())

        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder, times(1)).showTimeTillRevert(anyInt())
    }

    @Test fun `revert changes after timeout`() = Fixture().run {
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder).revertCurrentChangeList()
        ide.expect(inOrder).notifyThatChangesWereReverted()
    }

    @Test fun `reset timer after commit`() = Fixture().run {
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder).showTimeTillRevert(eq(1))

        autoRevert.onAllChangesCommitted()
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder).showTimeTillRevert(eq(1))
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder).showTimeTillRevert(eq(0))
    }

    @Test fun `stop timer if there are no changes`() = Fixture().run {
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 10))
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = false)
        ide.expect().showThatAutoRevertStopped()
    }
}

class AutoRevertPauseTests {
    @Test fun `pause timer when some changes are made`() = Fixture().run {
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder, times(1)).showTimeTillRevert(anyInt())

        autoRevert.onPause()

        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())
    }
}

class AutoRevertCommitDialogTests {
    @Test fun `when commit dialog is open, don't revert changes`() = Fixture().run {
        autoRevert.onTimer(hasChanges = true)
        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = true)

        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `after commit dialog is closed, revert pending changes`() = Fixture().run {
        autoRevert.onTimer(hasChanges = true)
        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(hasChanges = true)

        whenCalled(ide.isCommitDialogOpen()).thenReturn(false)
        autoRevert.onTimer(hasChanges = true)
        ide.expect().revertCurrentChangeList()
        ide.expect().notifyThatChangesWereReverted()
    }

    @Test fun `when commit dialog is open, don't start timer after timeout`() = Fixture().run {
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder, times(1)).showTimeTillRevert(eq(1))

        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder).showTimeTillRevert(eq(0))
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())
    }
}

class AutoRevertUpdateSettingsTests {
    private val Fixture.newSettings: Settings
        get() = settings.copy(secondsTillRevert = 0)

    @Test fun `use updated 'secondsTillRevert' settings`() = Fixture().run {
        autoRevert.onSettingsUpdate(newSettings)
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = true)

        ide.expect(times(2)).revertCurrentChangeList()
        ide.expect(times(2)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated 'secondsTillRevert' settings after the end of the current timeout`() = Fixture().run {
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onSettingsUpdate(newSettings) // settings not applied yet
        autoRevert.onTimer(hasChanges = true) // reverts changes after 2nd time event; settings applied
        autoRevert.onTimer(hasChanges = true) // reverts changes after 1st time event
        autoRevert.onTimer(hasChanges = true) // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated 'secondsTillRevert' settings after commit`() = Fixture().run {
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onSettingsUpdate(newSettings) // settings not applied yet
        autoRevert.onAllChangesCommitted() // settings applied
        autoRevert.onTimer(hasChanges = true) // reverts changes after 1st time event
        autoRevert.onTimer(hasChanges = true) // reverts changes after 1st time event
        autoRevert.onTimer(hasChanges = true) // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }
}

class AutoRevertDisabledTests {
    private val Fixture.disabledSettings: Settings
        get() = settings.copy(enabled = false)

    @Test fun `when disabled, don't show anything in UI`() = Fixture().run {
        autoRevert.onSettingsUpdate(disabledSettings)
        autoRevert.onTimer(hasChanges = true)

        ide.expect().onSettingsUpdate(settings)
        ide.expect().onSettingsUpdate(disabledSettings)
        ide.expectNoMoreInteractions()
    }

    @Test fun `when disabled while timer is running, don't revert changes`() = Fixture().run {
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onSettingsUpdate(disabledSettings)
        autoRevert.onTimer(hasChanges = true)

        ide.expect(never()).revertCurrentChangeList()
    }
}
