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
	private final Model model = new Model(ideNotifications, ideActions, CHANGE_TIMEOUT_IN_SECS);


	@Test public void whenStarted_Should_SendAutoRevertMessageToUI() {
		model.start();

		verify(ideNotifications).onAutoRevertStarted(eq(CHANGE_TIMEOUT_IN_SECS));
		verifyZeroInteractions(ideActions);
	}

	@Test public void shouldOnlySendNotificationEventsWhenModelStarted() {
		InOrder inOrder = inOrder(ideNotifications);

		model.onTimer(); inOrder.verify(ideNotifications, times(0)).onTimer(anyInt());
		model.start();
		model.onTimer(); inOrder.verify(ideNotifications).onTimer(anyInt());
	}

	@Test public void whenStarted_And_ReceivesEnoughTimeUpdates_ShouldRevertCurrentChangeList() {
		model.start();

		callOnTimer(2 * CHANGE_TIMEOUT_IN_SECS);

		verify(ideActions, times(2)).revertCurrentChangeList();
		verifyNoMoreInteractions(ideActions);
	}

	@Test public void when_Stopped_Should_Not_Perform_Revert() {
		model.start();
		model.onTimer();
		model.stop();
		model.onTimer();

		verify(ideNotifications).onAutoRevertStarted(anyInt());
		verify(ideNotifications).onAutoRevertStopped();
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenStopped_should_ResetTimeLeftTillRevert_And_NotifyUI() {
		InOrder inOrder = inOrder(ideNotifications);

		model.start();
		model.onTimer(); inOrder.verify(ideNotifications).onTimer(eq(2));
		model.stop();
		model.start();
		model.onTimer(); inOrder.verify(ideNotifications).onTimer(eq(2));
		model.onTimer(); inOrder.verify(ideNotifications).onTimer(eq(1));
	}

	@Test public void no_Reverts_Performed_After_Commit() {
		model.start();
		model.onTimer();
		model.onCommit();
		model.onTimer();

		verify(ideNotifications).onAutoRevertStarted(anyInt());
		verifyZeroInteractions(ideActions);
	}

	@Test public void resets_Timer_And_NotifiesUI_On_Commit() {
		InOrder inOrder = inOrder(ideNotifications);

		model.start();
		model.onTimer();  inOrder.verify(ideNotifications).onTimer(2);
		model.onCommit(); inOrder.verify(ideNotifications).onCommit(CHANGE_TIMEOUT_IN_SECS);
		model.onTimer();  inOrder.verify(ideNotifications).onTimer(2);
		model.onTimer();  inOrder.verify(ideNotifications).onTimer(1);
	}

	@Test public void whenNotStarted_Does_NOT_NotifyUIAboutCommits() {
		model.onCommit();
		model.start();
		model.onCommit();

		verify(ideNotifications, times(1)).onCommit(anyInt());
	}

	@Test public void whenTimeOutChangesBeforeStarting_should_ApplyAtNextStart() {
		model.onNewSettings(1);
		model.start();
		model.onTimer();
		model.onTimer();

		verify(ideActions, times(2)).revertCurrentChangeList();
	}

	@Test public void whenTimeOutChangesAfterStart_should_ApplyItAfterEndOfCurrentTimeOut() {
		model.start();
		model.onNewSettings(1);
		model.onTimer();
		model.onTimer(); // reverts changes after 2nd time event
		model.onTimer(); // reverts changes after 1st time event
		model.onTimer(); // reverts changes after 1st time event

		verify(ideActions, times(3)).revertCurrentChangeList();
	}

	@Test public void whenTimeOutChangesAfterStart_should_ApplyItAfterNext_Commit() {
		model.start();
		model.onNewSettings(1);
		model.onTimer();
		model.onCommit();
		model.onTimer(); // reverts changes after 1st time event
		model.onTimer(); // reverts changes after 1st time event
		model.onTimer(); // reverts changes after 1st time event

		verify(ideActions, times(3)).revertCurrentChangeList();
	}

	private void callOnTimer(int numberOfTimes){
		for(int i = 0; i < numberOfTimes; i++){
			model.onTimer();
		}
	}
}
