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
import limitedwip.autorevert.components.IdeAdapter
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*


class AutoRevertTest {
    private val ideAdapter = mock(IdeAdapter::class.java)
    private val settings = Settings(true, secondsTillRevert, true)
    private val autoRevert = AutoRevert(ideAdapter).init(settings)
    private var seconds: Int = 0


    @Test fun `send UI startup notification`() {
        autoRevert.start()
        verify(ideAdapter).onAutoRevertStarted(eq(secondsTillRevert))
    }

    @Test fun `send UI notification on timer only when started`() {
        val inOrder = inOrder(ideAdapter)

        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter, times(0)).onTimeTillRevert(anyInt())
        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(anyInt())
    }

    @Test fun `revert changes when received enough time updates`() {
        autoRevert.start()

        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())

        verify(ideAdapter, times(2)).revertCurrentChangeList()
        verify(ideAdapter, times(2)).onChangesRevert()
    }

    @Test fun `don't revert changes when stopped`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.stop()
        autoRevert.onTimer(next())

        verify(ideAdapter).onAutoRevertStarted(anyInt())
        verify(ideAdapter).onAutoRevertStopped()
        verify(ideAdapter, never()).revertCurrentChangeList()
    }

    @Test fun `reset time till revert when stopped`() {
        val inOrder = inOrder(ideAdapter)

        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(eq(2))
        autoRevert.stop()
        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(eq(1))
    }

    @Test fun `reset time till revert when committed`() {
        val inOrder = inOrder(ideAdapter)

        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(eq(2))
        autoRevert.onAllFilesCommitted()
        inOrder.verify(ideAdapter).onCommit(secondsTillRevert)
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(eq(1))
    }

    @Test fun `send UI notification on commit only when started`() {
        val inOrder = inOrder(ideAdapter)

        autoRevert.onAllFilesCommitted()
        inOrder.verify(ideAdapter, times(0)).onCommit(anyInt())
        autoRevert.start()
        autoRevert.onAllFilesCommitted()
        inOrder.verify(ideAdapter).onCommit(anyInt())
    }

    @Test fun `apply revert time out change after start`() {
        autoRevert.onSettings(Settings(1))
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())

        verify(ideAdapter, times(2)).revertCurrentChangeList()
        verify(ideAdapter, times(2)).onChangesRevert()
    }

    @Test fun `apply revert timeout change after end of current time out`() {
        autoRevert.start()
        autoRevert.onSettings(Settings(1))
        autoRevert.onTimer(next())
        autoRevert.onTimer(next()) // reverts changes after 2nd time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event

        verify(ideAdapter, times(3)).revertCurrentChangeList()
        verify(ideAdapter, times(3)).onChangesRevert()
    }

    @Test fun `apply revert timeout change after commit`() {
        autoRevert.start()
        autoRevert.onSettings(Settings(1))
        autoRevert.onTimer(next())
        autoRevert.onAllFilesCommitted()
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event

        verify(ideAdapter, times(3)).revertCurrentChangeList()
        verify(ideAdapter, times(3)).onChangesRevert()
    }

    @Test fun `don't send UI startup notification when disabled`() {
        val disabledSettings = Settings(false, secondsTillRevert, false)
        autoRevert.onSettings(disabledSettings)
        autoRevert.start()

        verify(ideAdapter).onSettingsUpdate(settings)
        verify(ideAdapter).onSettingsUpdate(disabledSettings)
        verifyNoMoreInteractions(ideAdapter)
    }

    @Test fun `don't revert changes when disabled`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.onSettings(Settings(false, 2, false))
        autoRevert.onTimer(next())

        verify(ideAdapter, never()).revertCurrentChangeList()
    }

    @Before fun setUp() {
        `when`(ideAdapter.revertCurrentChangeList()).thenReturn(10)
    }

    private fun next(): Int {
        return ++seconds
    }

    companion object {
        private const val secondsTillRevert = 2
    }
}
