package limitedwip.autorevert

import limitedwip.autorevert.AutoRevert.Settings
import limitedwip.autorevert.components.Ide
import limitedwip.expect
import limitedwip.expectNoMoreInteractions
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

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

    @Test fun `send UI startup notification`() {
        autoRevert.start()
        ide.expect().showTimeTillRevert(eq(secondsTillRevert))
    }

    @Test fun `send UI notification on timer only when started`() {
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(2)).showTimeTillRevert(anyInt())
    }

    @Test fun `revert changes after each timeout`() {
        autoRevert.start()

        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        ide.expect(inOrder).revertCurrentChangeList()
        ide.expect(inOrder).notifyThatChangesWereReverted()

        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        ide.expect(inOrder).revertCurrentChangeList()
        ide.expect(inOrder).notifyThatChangesWereReverted()
    }

    @Test fun `don't revert changes while commit dialog is open`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        `when`(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(next())
        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `revert changes after commit dialog is closed`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        `when`(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(next())

        `when`(ide.isCommitDialogOpen()).thenReturn(false)
        autoRevert.onTimer(next())
        ide.expect().revertCurrentChangeList()
        ide.expect().notifyThatChangesWereReverted()
    }

    @Test fun `don't start timer when timeout with commit dialog open`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(2)).showTimeTillRevert(eq(2))

        `when`(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(next())
        ide.expect(inOrder).showTimeTillRevert(eq(1))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showTimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showTimeTillRevert(eq(2))

        `when`(ide.isCommitDialogOpen()).thenReturn(false)
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        ide.expect(inOrder).showTimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showTimeTillRevert(eq(1))
    }

    @Test fun `don't revert changes when stopped`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.stop()
        autoRevert.onTimer(next())

        ide.expect().showThatAutoRevertStopped()
        ide.expect(never()).revertCurrentChangeList()
    }

    @Test fun `reset timer when stopped`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(2)).showTimeTillRevert(eq(2))
        autoRevert.stop()

        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(2)).showTimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showTimeTillRevert(eq(1))
    }

    @Test fun `reset timer when committed`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(2)).showTimeTillRevert(eq(2))

        autoRevert.onAllChangesCommitted()
        ide.expect(inOrder).showTimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showTimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showTimeTillRevert(eq(1))
    }

    @Test fun `send UI notification on commit only when started`() {
        autoRevert.onAllChangesCommitted()
        ide.expect(inOrder, times(0)).showTimeTillRevert(anyInt())

        autoRevert.start()
        autoRevert.onAllChangesCommitted()
        ide.expect(inOrder, times(2)).showTimeTillRevert(anyInt())
    }

    @Test fun `use updated revert timeout after start`() {
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1))
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())

        ide.expect(times(2)).revertCurrentChangeList()
        ide.expect(times(2)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated revert timeout after end of the current timeout`() {
        autoRevert.start()
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1))
        autoRevert.onTimer(next())
        autoRevert.onTimer(next()) // reverts changes after 2nd time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `use updated revert timeout after commit`() {
        autoRevert.start()
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1))
        autoRevert.onTimer(next())
        autoRevert.onAllChangesCommitted()
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).notifyThatChangesWereReverted()
    }

    @Test fun `don't send UI startup notification when disabled`() {
        val disabledSettings = Settings(false, secondsTillRevert, false)
        autoRevert.onSettingsUpdate(disabledSettings)
        autoRevert.start()

        ide.expect().onSettingsUpdate(settings)
        ide.expect().onSettingsUpdate(disabledSettings)
        ide.expectNoMoreInteractions()
    }

    @Test fun `don't revert changes when disabled`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.onSettingsUpdate(Settings(false, 2, false))
        autoRevert.onTimer(next())

        ide.expect(never()).revertCurrentChangeList()
    }

    @Before fun setUp() {
        `when`(ide.revertCurrentChangeList()).thenReturn(10)
    }

    private fun next(): Int = ++seconds
}
