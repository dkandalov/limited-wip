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
    private val secondsTillRevert = 2
    private val ide = mock(Ide::class.java)
    private val inOrder = inOrder(ide)
    private val settings = Settings(
        autoRevertEnabled = true,
        secondsTillRevert = secondsTillRevert,
        notifyOnRevert = true
    )
    private val autoRevert = AutoRevert(ide, settings)
    private var seconds: Int = 0

    @Test fun `show time till revert when started`() {
        autoRevert.onNextTimer()
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())

        autoRevert.onTimerWithChanges()
        ide.expect(inOrder, times(1)).showTimeTillRevert(anyInt())
    }

    @Test fun `automatically start on timer update with some changes`() {
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

    @Test fun `don't revert changes while commit dialog is open`() {
        autoRevert.onTimerWithChanges()
        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimerWithChanges()
        autoRevert.onTimerWithChanges()

        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `revert changes after commit dialog is closed`() {
        autoRevert.onTimerWithChanges()
        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimerWithChanges()

        whenCalled(ide.isCommitDialogOpen()).thenReturn(false)
        autoRevert.onTimerWithChanges()
        ide.expect().revertCurrentChangeList()
        ide.expect().notifyThatChangesWereReverted()
    }

    @Test fun `don't start timer after timeout with commit dialog open`() {
        autoRevert.onTimerWithChanges()
        ide.expect(inOrder, times(1)).showTimeTillRevert(eq(2))

        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onNextTimer()
        ide.expect(inOrder).showTimeTillRevert(eq(1))
        autoRevert.onNextTimer()
        ide.expect(inOrder).showTimeTillRevert(eq(2))
        autoRevert.onNextTimer()
        ide.expect(inOrder).showTimeTillRevert(eq(2)) // timer is still 2

        whenCalled(ide.isCommitDialogOpen()).thenReturn(false)
        autoRevert.onNextTimer()
        autoRevert.onNextTimer()
        ide.expect(inOrder).showTimeTillRevert(eq(2))
        autoRevert.onNextTimer()
        ide.expect(inOrder).showTimeTillRevert(eq(1))
    }

    @Test fun `reset timer on commit`() {
        autoRevert.onTimerWithChanges()
        ide.expect(inOrder, times(1)).showTimeTillRevert(eq(2))

        autoRevert.onAllChangesCommitted()
        ide.expect(inOrder).showTimeTillRevert(eq(2))
        autoRevert.onNextTimer()
        ide.expect(inOrder).showTimeTillRevert(eq(2))
        autoRevert.onNextTimer()
        ide.expect(inOrder).showTimeTillRevert(eq(1))
    }

    @Test fun `use updated settings`() {
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1))
        autoRevert.onTimerWithChanges()
        autoRevert.onTimerWithChanges()

        ide.expect(times(2)).revertCurrentChangeList()
        ide.expect(times(2)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated settings after the end of the current timeout`() {
        autoRevert.onTimerWithChanges()
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1)) // settings not applied yet
        autoRevert.onTimerWithChanges() // reverts changes after 2nd time event; settings applied
        autoRevert.onTimerWithChanges() // reverts changes after 1st time event
        autoRevert.onTimerWithChanges() // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated settings after commit`() {
        autoRevert.onTimerWithChanges()
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1)) // settings not applied yet
        autoRevert.onAllChangesCommitted() // settings applied
        autoRevert.onTimerWithChanges() // reverts changes after 1st time event
        autoRevert.onTimerWithChanges() // reverts changes after 1st time event
        autoRevert.onTimerWithChanges() // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `don't show anything in UI when disabled`() {
        val disabledSettings = Settings(false, secondsTillRevert, false)
        autoRevert.onSettingsUpdate(disabledSettings)
        autoRevert.onTimerWithChanges()

        ide.expect().onSettingsUpdate(settings)
        ide.expect().onSettingsUpdate(disabledSettings)
        ide.expectNoMoreInteractions()
    }

    @Test fun `don't revert changes when disabled`() {
        autoRevert.onTimerWithChanges()
        autoRevert.onSettingsUpdate(Settings(false, 2, false))
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
