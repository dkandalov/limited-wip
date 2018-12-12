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
        ide.expect().showInUIThatAutoRevertStopped(eq(secondsTillRevert))
    }

    @Test fun `send UI notification on timer only when started`() {
        autoRevert.onTimer(next())
        ide.expect(inOrder, times(0)).showInUITimeTillRevert(anyInt())
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(anyInt())
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

    @Test fun `don't revert changes when stopped`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.stop()
        autoRevert.onTimer(next())

        ide.expect().showInUIThatAutoRevertStopped(anyInt())
        ide.expect().showInUIThatAutoRevertStopped()
        ide.expect(never()).revertCurrentChangeList()
    }

    @Test fun `reset timer when stopped`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(2))
        autoRevert.stop()

        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(1))
    }

    @Test fun `reset timer when committed`() {
        autoRevert.start()
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(2))

        autoRevert.onAllFilesCommitted()
        ide.expect(inOrder).showInUITimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        ide.expect(inOrder).showInUITimeTillRevert(eq(1))
    }

    @Test fun `send UI notification on commit only when started`() {
        autoRevert.onAllFilesCommitted()
        ide.expect(inOrder, times(0)).showInUITimeTillRevert(anyInt())

        autoRevert.start()
        autoRevert.onAllFilesCommitted()
        ide.expect(inOrder).showInUITimeTillRevert(anyInt())
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
        autoRevert.onAllFilesCommitted()
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
