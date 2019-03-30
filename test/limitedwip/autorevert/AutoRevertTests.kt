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
    private var seconds = 0

    @Test fun `start timer when some changes are made`() {
        autoRevert.onNextTimer()
        autoRevert.onNextTimer()
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())

        autoRevert.onTimerWithChanges()
        ide.expect(inOrder, times(1)).showTimeTillRevert(anyInt())
    }

    @Test fun `revert changes after timeout`() {
        autoRevert.onTimerWithChanges()
        autoRevert.onTimerWithChanges()
        ide.expect(inOrder).revertCurrentChangeList()
        ide.expect(inOrder).notifyThatChangesWereReverted()
    }

    @Test fun `reset timer after commit`() {
        autoRevert.onTimerWithChanges()
        ide.expect(inOrder).showTimeTillRevert(eq(1))

        autoRevert.onAllChangesCommitted()
        autoRevert.onTimerWithChanges()
        ide.expect(inOrder).showTimeTillRevert(eq(1))
        autoRevert.onTimerWithChanges()
        ide.expect(inOrder).showTimeTillRevert(eq(0))
    }

    @Test fun `stop timer if there are no changes`() {
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 10))
        autoRevert.onTimerWithChanges()
        autoRevert.onNextTimer()
        ide.expect().showThatAutoRevertStopped()
    }

    @Test fun `when commit dialog is open, don't revert changes`() {
        autoRevert.onTimerWithChanges()
        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimerWithChanges()
        autoRevert.onTimerWithChanges()
        autoRevert.onTimerWithChanges()

        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `after commit dialog is closed, revert pending changes`() {
        autoRevert.onTimerWithChanges()
        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimerWithChanges()

        whenCalled(ide.isCommitDialogOpen()).thenReturn(false)
        autoRevert.onTimerWithChanges()
        ide.expect().revertCurrentChangeList()
        ide.expect().notifyThatChangesWereReverted()
    }

    @Test fun `when commit dialog is open, don't start timer after timeout`() {
        ide.expect().onSettingsUpdate(settings)

        autoRevert.onTimerWithChanges()
        ide.expect(inOrder, times(1)).showTimeTillRevert(eq(1))

        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimerWithChanges()
        ide.expect(inOrder).showTimeTillRevert(eq(0))
        autoRevert.onTimerWithChanges()
        autoRevert.onTimerWithChanges()
        autoRevert.onTimerWithChanges()
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())
    }

    @Test fun `use updated 'secondsTillRevert' settings`() {
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 0))
        autoRevert.onTimerWithChanges()
        autoRevert.onTimerWithChanges()

        ide.expect(times(2)).revertCurrentChangeList()
        ide.expect(times(2)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated 'secondsTillRevert' settings after the end of the current timeout`() {
        autoRevert.onTimerWithChanges()
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 0)) // settings not applied yet
        autoRevert.onTimerWithChanges() // reverts changes after 2nd time event; settings applied
        autoRevert.onTimerWithChanges() // reverts changes after 1st time event
        autoRevert.onTimerWithChanges() // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated 'secondsTillRevert' settings after commit`() {
        autoRevert.onTimerWithChanges()
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 0)) // settings not applied yet
        autoRevert.onAllChangesCommitted() // settings applied
        autoRevert.onTimerWithChanges() // reverts changes after 1st time event
        autoRevert.onTimerWithChanges() // reverts changes after 1st time event
        autoRevert.onTimerWithChanges() // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `when disabled, don't show anything in UI`() {
        val disabledSettings = settings.copy(autoRevertEnabled = false)
        autoRevert.onSettingsUpdate(disabledSettings)
        autoRevert.onTimerWithChanges()

        ide.expect().onSettingsUpdate(settings)
        ide.expect().onSettingsUpdate(disabledSettings)
        ide.expectNoMoreInteractions()
    }

    @Test fun `when disabled while timer is running, don't revert changes`() {
        val disabledSettings = settings.copy(autoRevertEnabled = false)
        autoRevert.onTimerWithChanges()
        autoRevert.onSettingsUpdate(disabledSettings)
        autoRevert.onTimerWithChanges()

        ide.expect(never()).revertCurrentChangeList()
    }

    @Before fun setUp() {
        whenCalled(ide.revertCurrentChangeList()).thenReturn(10)
    }

    private fun AutoRevert.onTimerWithChanges() = onTimer(next(), hasChanges = true)

    private fun AutoRevert.onNextTimer(hasChanges: Boolean = false) = onTimer(next(), hasChanges)

    private fun next(): Int = ++seconds
}
