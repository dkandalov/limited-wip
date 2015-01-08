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
package limitedwip;

import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;


public class AutoRevertTest {
	private static final int CHANGE_TIMEOUT_IN_SECS = 2;

	private final IdeNotifications ideNotifications = mock(IdeNotifications.class);
	private final IdeActions ideActions = mock(IdeActions.class);
	private final AutoRevert autoRevert = new AutoRevert(ideNotifications, ideActions, CHANGE_TIMEOUT_IN_SECS);


	@Test public void whenStarted_Should_SendAutoRevertMessageToUI() {
		autoRevert.start();

		verify(ideNotifications).onAutoRevertStarted(eq(CHANGE_TIMEOUT_IN_SECS));
		verifyZeroInteractions(ideActions);
	}

	@Test public void shouldOnlySendNotificationEventsWhenModelStarted() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.onTimer(); inOrder.verify(ideNotifications, times(0)).onTimer(anyInt());
		autoRevert.start();
		autoRevert.onTimer(); inOrder.verify(ideNotifications).onTimer(anyInt());
	}

	@Test public void whenStarted_And_ReceivesEnoughTimeUpdates_ShouldRevertCurrentChangeList() {
		autoRevert.start();

		callOnTimer(2 * CHANGE_TIMEOUT_IN_SECS);

		verify(ideActions, times(2)).revertCurrentChangeList();
		verifyNoMoreInteractions(ideActions);
	}

	@Test public void when_Stopped_Should_Not_Perform_Revert() {
		autoRevert.start();
		autoRevert.onTimer();
		autoRevert.stop();
		autoRevert.onTimer();

		verify(ideNotifications).onAutoRevertStarted(anyInt());
		verify(ideNotifications).onAutoRevertStopped();
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenStopped_should_ResetTimeLeftTillRevert_And_NotifyUI() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.start();
		autoRevert.onTimer(); inOrder.verify(ideNotifications).onTimer(eq(2));
		autoRevert.stop();
		autoRevert.start();
		autoRevert.onTimer(); inOrder.verify(ideNotifications).onTimer(eq(2));
		autoRevert.onTimer(); inOrder.verify(ideNotifications).onTimer(eq(1));
	}

	@Test public void no_Reverts_Performed_After_Commit() {
		autoRevert.start();
		autoRevert.onTimer();
		autoRevert.onCommit();
		autoRevert.onTimer();

		verify(ideNotifications).onAutoRevertStarted(anyInt());
		verifyZeroInteractions(ideActions);
	}

	@Test public void resets_Timer_And_NotifiesUI_On_Commit() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.start();
		autoRevert.onTimer();  inOrder.verify(ideNotifications).onTimer(2);
		autoRevert.onCommit(); inOrder.verify(ideNotifications).onCommit(CHANGE_TIMEOUT_IN_SECS);
		autoRevert.onTimer();  inOrder.verify(ideNotifications).onTimer(2);
		autoRevert.onTimer();  inOrder.verify(ideNotifications).onTimer(1);
	}

	@Test public void whenNotStarted_Does_NOT_NotifyUIAboutCommits() {
		autoRevert.onCommit();
		autoRevert.start();
		autoRevert.onCommit();

		verify(ideNotifications, times(1)).onCommit(anyInt());
	}

	@Test public void whenTimeOutChangesBeforeStarting_should_ApplyAtNextStart() {
		autoRevert.onNewSettings(1);
		autoRevert.start();
		autoRevert.onTimer();
		autoRevert.onTimer();

		verify(ideActions, times(2)).revertCurrentChangeList();
	}

	@Test public void whenTimeOutChangesAfterStart_should_ApplyItAfterEndOfCurrentTimeOut() {
		autoRevert.start();
		autoRevert.onNewSettings(1);
		autoRevert.onTimer();
		autoRevert.onTimer(); // reverts changes after 2nd time event
		autoRevert.onTimer(); // reverts changes after 1st time event
		autoRevert.onTimer(); // reverts changes after 1st time event

		verify(ideActions, times(3)).revertCurrentChangeList();
	}

	@Test public void whenTimeOutChangesAfterStart_should_ApplyItAfterNext_Commit() {
		autoRevert.start();
		autoRevert.onNewSettings(1);
		autoRevert.onTimer();
		autoRevert.onCommit();
		autoRevert.onTimer(); // reverts changes after 1st time event
		autoRevert.onTimer(); // reverts changes after 1st time event
		autoRevert.onTimer(); // reverts changes after 1st time event

		verify(ideActions, times(3)).revertCurrentChangeList();
	}

	private void callOnTimer(int numberOfTimes){
		for(int i = 0; i < numberOfTimes; i++){
			autoRevert.onTimer();
		}
	}
}
