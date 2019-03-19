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
    private val settings = Settings(true, secondsTillRevert, true)
    private val autoRevert = AutoRevert(ide, settings)
    private var seconds: Int = 0

    @Test fun `send UI startup notification`() {
        autoRevert.start()
        ide.expect().showInUITimeTillRevert(eq(secondsTillRevert))
    }

    @Test fun `send UI notification on timer only when started`() {
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(0)).showInUITimeTillRevert(anyInt())
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(2)).showInUITimeTillRevert(anyInt())
    }

    @Test fun `revert changes after each timeout`() {
        autoRevert.start()

        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        ide.expect(inOrder).revertCurrentChangeList()
        ide.expect(inOrder).showNotificationThatChangesWereReverted()

        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        ide.expect(inOrder).revertCurrentChangeList()
        ide.expect(inOrder).showNotificationThatChangesWereReverted()
    }

    @Test fun `don't revert changes while commit dialog is open`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        `when`(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(next())
        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).showNotificationThatChangesWereReverted()
    }

    @Test fun `revert changes after commit dialog is closed`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        `when`(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(next())

        `when`(ide.isCommitDialogOpen()).thenReturn(false)
        autoRevert.onTimer(next())
        ide.expect().revertCurrentChangeList()
        ide.expect().showNotificationThatChangesWereReverted()
    }

    @Test fun `don't start timer when timeout with commit dialog open`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(2)).showInUITimeTillRevert(eq(2))

        `when`(ide.isCommitDialogOpen()).thenReturn(true)
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(1))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(2))

        `when`(ide.isCommitDialogOpen()).thenReturn(false)
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(1))
    }

    @Test fun `don't revert changes when stopped`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.stop()
        autoRevert.onTimer(next())

        ide.expect().showInUIThatAutoRevertStopped()
        ide.expect(never()).revertCurrentChangeList()
    }

    @Test fun `reset timer when stopped`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(2)).showInUITimeTillRevert(eq(2))
        autoRevert.stop()

        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(2)).showInUITimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(1))
    }

    @Test fun `reset timer when committed`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(2)).showInUITimeTillRevert(eq(2))

        autoRevert.onAllChangesCommitted()
        ide.expect(inOrder).showInUITimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(1))
    }

    @Test fun `send UI notification on commit only when started`() {
        autoRevert.onAllChangesCommitted()
        ide.expect(inOrder, times(0)).showInUITimeTillRevert(anyInt())

        autoRevert.start()
        autoRevert.onAllChangesCommitted()
        ide.expect(inOrder, times(2)).showInUITimeTillRevert(anyInt())
    }

    @Test fun `use updated revert timeout after start`() {
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1))
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())

        ide.expect(times(2)).revertCurrentChangeList()
        ide.expect(times(2)).showNotificationThatChangesWereReverted()
    }

    @Test fun `use updated revert timeout after end of the current timeout`() {
        autoRevert.start()
        autoRevert.onSettingsUpdate(settings.copy(secondsTillRevert = 1))
        autoRevert.onTimer(next())
        autoRevert.onTimer(next()) // reverts changes after 2nd time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event

        ide.expect(times(3)).revertCurrentChangeList()
        ide.expect(times(3)).showNotificationThatChangesWereReverted()
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
        ide.expect(times(3)).showNotificationThatChangesWereReverted()
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
