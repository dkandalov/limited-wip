package limitedwip.autorevert

import limitedwip.autorevert.AutoRevert.Settings
import limitedwip.autorevert.components.Ide
import limitedwip.expect
import limitedwip.expectNoMoreInteractions
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.Mockito.`when` as whenCalled

class AutoRevertTests {
    private val ide = mock(Ide::class.java)
    private val inOrder = inOrder(ide)
    private val settings = Settings(
        autoRevertEnabled = true,
        secondsTillRevert = 1,
        notifyOnRevert = true
    )
    private val autoRevert = AutoRevert(ide, settings)

    @Test fun `start timer when some changes are made`() {
        autoRevert.onTimer(hasChanges = false)
        autoRevert.onTimer(hasChanges = false)
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())

        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder, times(1)).showTimeTillRevert(anyInt())
    }

    @Test fun `revert changes after timeout`() {
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder).revertCurrentChangeList()
        ide.expect(inOrder).notifyThatChangesWereReverted()
    }

    @Test fun `reset timer after commit`() {
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder).showTimeTillRevert(eq(1))

        autoRevert.onAllChangesCommitted()
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder).showTimeTillRevert(eq(1))
        autoRevert.onTimer(hasChanges = true)
        ide.expect(inOrder).showTimeTillRevert(eq(0))
    }

    @Test fun `stop timer if there are no changes`() {
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 10))
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = false)
        ide.expect().showThatAutoRevertStopped()
    }

    @Test fun `when commit dialog is open, don't revert changes`() {
        autoRevert.onTimer(hasChanges = true)
        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = true)

        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `after commit dialog is closed, revert pending changes`() {
        autoRevert.onTimer(hasChanges = true)
        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(hasChanges = true)

        whenCalled(ide.isCommitDialogOpen()).thenReturn(false)
        autoRevert.onTimer(hasChanges = true)
        ide.expect().revertCurrentChangeList()
        ide.expect().notifyThatChangesWereReverted()
    }

    @Test fun `when commit dialog is open, don't start timer after timeout`() {
        ide.expect().onSettingsUpdate(settings)

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

    @Test fun `use updated 'secondsTillRevert' settings`() {
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 0))
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onTimer(hasChanges = true)

        ide.expect(times(2)).revertCurrentChangeList()
        ide.expect(times(2)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated 'secondsTillRevert' settings after the end of the current timeout`() {
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 0)) // settings not applied yet
        autoRevert.onTimer(hasChanges = true) // reverts changes after 2nd time event; settings applied
        autoRevert.onTimer(hasChanges = true) // reverts changes after 1st time event
        autoRevert.onTimer(hasChanges = true) // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated 'secondsTillRevert' settings after commit`() {
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 0)) // settings not applied yet
        autoRevert.onAllChangesCommitted() // settings applied
        autoRevert.onTimer(hasChanges = true) // reverts changes after 1st time event
        autoRevert.onTimer(hasChanges = true) // reverts changes after 1st time event
        autoRevert.onTimer(hasChanges = true) // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `when disabled, don't show anything in UI`() {
        val disabledSettings = settings.copy(autoRevertEnabled = false)
        autoRevert.onSettingsUpdate(disabledSettings)
        autoRevert.onTimer(hasChanges = true)

        ide.expect().onSettingsUpdate(settings)
        ide.expect().onSettingsUpdate(disabledSettings)
        ide.expectNoMoreInteractions()
    }

    @Test fun `when disabled while timer is running, don't revert changes`() {
        val disabledSettings = settings.copy(autoRevertEnabled = false)
        autoRevert.onTimer(hasChanges = true)
        autoRevert.onSettingsUpdate(disabledSettings)
        autoRevert.onTimer(hasChanges = true)

        ide.expect(never()).revertCurrentChangeList()
    }

    @Before fun setUp() {
        whenCalled(ide.revertCurrentChangeList()).thenReturn(10)
    }
}
