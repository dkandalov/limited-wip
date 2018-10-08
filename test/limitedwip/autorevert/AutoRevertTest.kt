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
import org.mockito.Matchers
import org.mockito.Mockito.*


class AutoRevertTest {
    private val ideAdapter = mock(IdeAdapter::class.java)
    private val settings = Settings(true, secondsTillRevert, true)
    private val autoRevert = AutoRevert(ideAdapter).init(settings)
    private var secondsSinceStart: Int = 0


    @Test fun sendsUIStartupNotification() {
        autoRevert.start()
        verify(ideAdapter).onAutoRevertStarted(eq(secondsTillRevert))
    }

    @Test fun sendsUINotificationOnTimer_OnlyWhenStarted() {
        val inOrder = inOrder(ideAdapter)

        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter, times(0)).onTimeTillRevert(anyInt())
        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(anyInt())
    }

    @Test fun revertsChanges_WhenReceivedEnoughTimeUpdates() {
        autoRevert.start()

        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())

        verify(ideAdapter, times(2)).revertCurrentChangeList()
        verify(ideAdapter, times(2)).onChangesRevert()
    }

    @Test fun doesNotRevertChanges_WhenStopped() {
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.stop()
        autoRevert.onTimer(next())

        verify(ideAdapter).onAutoRevertStarted(anyInt())
        verify(ideAdapter).onAutoRevertStopped()
        verify(ideAdapter, never()).revertCurrentChangeList()
    }

    @Test fun resetsTimeTillRevert_WhenStopped() {
        val inOrder = inOrder(ideAdapter)

        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(eq(2))
        autoRevert.stop()
        autoRevert.start()
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(eq(2))
        autoRevert.onTimer(next())
        inOrder.verify(ideAdapter).onTimeTillRevert(Matchers.eq(1))
    }

    @Test fun resetsTimeTillRevert_WhenCommitted() {
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

    @Test fun sendsUINotificationOnCommit_OnlyWhenStarted() {
        val inOrder = inOrder(ideAdapter)

        autoRevert.onAllFilesCommitted()
        inOrder.verify(ideAdapter, times(0)).onCommit(anyInt())
        autoRevert.start()
        autoRevert.onAllFilesCommitted()
        inOrder.verify(ideAdapter).onCommit(Matchers.anyInt())
    }

    @Test fun appliesRevertTimeOutChange_AfterStart() {
        autoRevert.onSettings(Settings(1))
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.onTimer(next())

        verify(ideAdapter, times(2)).revertCurrentChangeList()
        verify(ideAdapter, times(2)).onChangesRevert()
    }

    @Test fun appliesRevertTimeoutChange_AfterEndOfCurrentTimeOut() {
        autoRevert.start()
        autoRevert.onSettings(Settings(1))
        autoRevert.onTimer(next())
        autoRevert.onTimer(next()) // reverts changes after 2nd time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event
        autoRevert.onTimer(next()) // reverts changes after 1st time event

        verify(ideAdapter, times(3)).revertCurrentChangeList()
        verify(ideAdapter, times(3)).onChangesRevert()
    }

    @Test fun appliesRevertTimeoutChange_AfterCommit() {
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

    @Test fun doesNotSendUIStartupNotification_WhenDisabled() {
        val disabledSettings = Settings(false, secondsTillRevert, false)
        autoRevert.onSettings(disabledSettings)
        autoRevert.start()

        verify(ideAdapter).onSettingsUpdate(settings)
        verify(ideAdapter).onSettingsUpdate(disabledSettings)
        verifyNoMoreInteractions(ideAdapter)
    }

    @Test fun doesNotRevertChanges_WhenDisabled() {
        autoRevert.start()
        autoRevert.onTimer(next())
        autoRevert.onSettings(Settings(false, 2, false))
        autoRevert.onTimer(next())

        verify(ideAdapter, never()).revertCurrentChangeList()
    }

    @Before @Throws(Exception::class)
    fun setUp() {
        secondsSinceStart = 0
        stub(ideAdapter.revertCurrentChangeList()).toReturn(10)
    }

    private operator fun next(): Int {
        return ++secondsSinceStart
    }

    companion object {
        private val secondsTillRevert = 2
    }
}
