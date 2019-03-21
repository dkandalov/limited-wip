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
        autoRevert.start()
        ide.expect().showTimeTillRevert(eq(secondsTillRevert))
    }
    
    @Test fun `don't show time till revert before start`() {
        autoRevert.onNextTimer()
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())

        autoRevert.start()
        autoRevert.onNextTimer()
        ide.expect(inOrder, times(2)).showTimeTillRevert(anyInt())
    }

    @Test fun `revert changes after timeout`() {
        autoRevert.start()
        autoRevert.onNextTimer()
        autoRevert.onNextTimer()
        ide.expect(inOrder).revertCurrentChangeList()
        ide.expect(inOrder).notifyThatChangesWereReverted()
    }

    @Test fun `don't revert changes while commit dialog is open`() {
        autoRevert.start()
        autoRevert.onNextTimer()
        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onNextTimer()
        autoRevert.onNextTimer()

        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `revert changes after commit dialog is closed`() {
        autoRevert.start()
        autoRevert.onNextTimer()
        whenCalled(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onNextTimer()

        whenCalled(ide.isCommitDialogOpen()).thenReturn(false)
        autoRevert.onNextTimer()
        ide.expect().revertCurrentChangeList()
        ide.expect().notifyThatChangesWereReverted()
    }

    @Test fun `don't start timer after timeout with commit dialog open`() {
        autoRevert.start()
        autoRevert.onNextTimer()
        ide.expect(inOrder, times(2)).showTimeTillRevert(eq(2))

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

    @Test fun `don't revert changes when stopped`() {
        autoRevert.start()
        autoRevert.stop()
        autoRevert.onNextTimer()
        autoRevert.onNextTimer()

        ide.expect().showThatAutoRevertStopped()
        ide.expect(never()).revertCurrentChangeList()
    }

    @Test fun `reset timer when stopped`() {
        autoRevert.start()
        autoRevert.onNextTimer()
        ide.expect(inOrder, times(2)).showTimeTillRevert(eq(2))
        autoRevert.stop()

        autoRevert.start()
        autoRevert.onNextTimer()
        ide.expect(inOrder, times(2)).showTimeTillRevert(eq(2))
        autoRevert.onNextTimer()
        ide.expect(inOrder).showTimeTillRevert(eq(1))
    }

    @Test fun `reset timer on commit`() {
        autoRevert.start()
        autoRevert.onNextTimer()
        ide.expect(inOrder, times(2)).showTimeTillRevert(eq(2))

        autoRevert.onAllChangesCommitted()
        ide.expect(inOrder).showTimeTillRevert(eq(2))
        autoRevert.onNextTimer()
        ide.expect(inOrder).showTimeTillRevert(eq(2))
        autoRevert.onNextTimer()
        ide.expect(inOrder).showTimeTillRevert(eq(1))
    }

    @Test fun `on commit show time till revert only when started`() {
        autoRevert.onAllChangesCommitted()
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())

        autoRevert.start()
        autoRevert.onAllChangesCommitted()
        ide.expect(inOrder, times(2)).showTimeTillRevert(anyInt())
    }

    @Test fun `use updated revert timeout after start`() {
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1))
        autoRevert.start()
        autoRevert.onNextTimer()
        autoRevert.onNextTimer()

        ide.expect(times(2)).revertCurrentChangeList()
        ide.expect(times(2)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated revert timeout after the end of the current timeout`() {
        autoRevert.start()
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1))
        autoRevert.onNextTimer()
        autoRevert.onNextTimer() // reverts changes after 2nd time event
        autoRevert.onNextTimer() // reverts changes after 1st time event
        autoRevert.onNextTimer() // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated revert timeout after commit`() {
        autoRevert.start()
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1))
        autoRevert.onNextTimer()
        autoRevert.onAllChangesCommitted()
        autoRevert.onNextTimer() // reverts changes after 1st time event
        autoRevert.onNextTimer() // reverts changes after 1st time event
        autoRevert.onNextTimer() // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `don't show anything in UI when disabled`() {
        val disabledSettings = Settings(false, secondsTillRevert, false)
        autoRevert.onSettingsUpdate(disabledSettings)
        autoRevert.start()

        ide.expect().onSettingsUpdate(settings)
        ide.expect().onSettingsUpdate(disabledSettings)
        ide.expectNoMoreInteractions()
    }

    @Test fun `don't revert changes when disabled`() {
        autoRevert.start()
        autoRevert.onNextTimer()
        autoRevert.onSettingsUpdate(Settings(false, 2, false))
        autoRevert.onNextTimer()

        ide.expect(never()).revertCurrentChangeList()
    }

    @Before fun setUp() {
        whenCalled(ide.revertCurrentChangeList()).thenReturn(10)
    }

    private fun AutoRevert.onNextTimer() = onTimer(next())
    
    private fun next(): Int = ++seconds
}
