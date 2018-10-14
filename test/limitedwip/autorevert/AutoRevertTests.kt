/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package limitedwip.autorevert

import limitedwip.autorevert.AutoRevert.Settings
import limitedwip.autorevert.components.Ide
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*


class AutoRevertTests {
    private val secondsTillRevert = 2
    private val ide = mock(Ide::class.java)
    private val settings = Settings(true, secondsTillRevert, true)
    private val autoRevert = AutoRevert(ide, settings)
    private var seconds: Int = 0


    @Test fun `send UI startup notification`() {
        autoRevert.start()
        verify(ide).onAutoRevertStarted(eq(secondsTillRevert))
    }

    @Test fun `send UI notification on timer only when started`() {
        val inOrder = inOrder(ide)

        autoRevert.onTimer(next())
        inOrder.verify(ide, times(0)).onTimeTillRevert(anyInt())
        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ide).onTimeTillRevert(anyInt())
    }

    @Test fun `revert changes when received enough time updates`() {
        autoRevert.start()

        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())

        verify(ide, times(2)).revertCurrentChangeList()
        verify(ide, times(2)).showNotificationThatChangesWereReverted()
    }

    @Test fun `don't revert changes when stopped`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.stop()
        autoRevert.onTimer(next())

        verify(ide).onAutoRevertStarted(anyInt())
        verify(ide).onAutoRevertStopped()
        verify(ide, never()).revertCurrentChangeList()
    }

    @Test fun `reset time till revert when stopped`() {
        val inOrder = inOrder(ide)

        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ide).onTimeTillRevert(eq(2))
        autoRevert.stop()
        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ide).onTimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        inOrder.verify(ide).onTimeTillRevert(eq(1))
    }

    @Test fun `reset time till revert when committed`() {
        val inOrder = inOrder(ide)

        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ide).onTimeTillRevert(eq(2))
        autoRevert.onAllFilesCommitted()
        inOrder.verify(ide).onCommit(secondsTillRevert)
        autoRevert.onTimer(next())
        inOrder.verify(ide).onTimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        inOrder.verify(ide).onTimeTillRevert(eq(1))
    }

    @Test fun `send UI notification on commit only when started`() {
        val inOrder = inOrder(ide)

        autoRevert.onAllFilesCommitted()
        inOrder.verify(ide, times(0)).onCommit(anyInt())
        autoRevert.start()
        autoRevert.onAllFilesCommitted()
        inOrder.verify(ide).onCommit(anyInt())
    }

    @Test fun `apply revert time out change after start`() {
        autoRevert.onSettings(settings.copy(secondsTillRevert = 1))
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())

        verify(ide, times(2)).revertCurrentChangeList()
        verify(ide, times(2)).showNotificationThatChangesWereReverted()
    }

    @Test fun `apply revert timeout change after end of current time out`() {
        autoRevert.start()
        autoRevert.onSettings(settings.copy(secondsTillRevert = 1))
        autoRevert.onTimer(next())
        autoRevert.onTimer(next()) // reverts changes after 2nd time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event

        verify(ide, times(3)).revertCurrentChangeList()
        verify(ide, times(3)).showNotificationThatChangesWereReverted()
    }

    @Test fun `apply revert timeout change after commit`() {
        autoRevert.start()
        autoRevert.onSettings(settings.copy(secondsTillRevert = 1))
        autoRevert.onTimer(next())
        autoRevert.onAllFilesCommitted()
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event

        verify(ide, times(3)).revertCurrentChangeList()
        verify(ide, times(3)).showNotificationThatChangesWereReverted()
    }

    @Test fun `don't send UI startup notification when disabled`() {
        val disabledSettings = Settings(false, secondsTillRevert, false)
        autoRevert.onSettings(disabledSettings)
        autoRevert.start()

        verify(ide).onSettingsUpdate(settings)
        verify(ide).onSettingsUpdate(disabledSettings)
        verifyNoMoreInteractions(ide)
    }

    @Test fun `don't revert changes when disabled`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.onSettings(Settings(false, 2, false))
        autoRevert.onTimer(next())

        verify(ide, never()).revertCurrentChangeList()
    }

    @Before fun setUp() {
        `when`(ide.revertCurrentChangeList()).thenReturn(10)
    }

    private fun next(): Int = ++seconds
}
