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

import limitedwip.AutoRevert.Settings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;


public class AutoRevertTest {
	private static final int secondsTillRevert = 2;

	private final IdeNotifications ideNotifications = mock(IdeNotifications.class);
	private final IdeActions ideActions = mock(IdeActions.class);
	private final Settings settings = new Settings(true, secondsTillRevert, true);
	private final AutoRevert autoRevert = new AutoRevert(ideNotifications, ideActions, settings);
	private int secondsSinceStart;


	@Test public void sendsUIStartupNotification() {
		autoRevert.start();

		verify(ideNotifications).onAutoRevertStarted(eq(secondsTillRevert));
		verifyZeroInteractions(ideActions);
	}

	@Test public void sendsUINotificationOnTimer_OnlyWhenStarted() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.onTimer(next()); inOrder.verify(ideNotifications, times(0)).onTimeTillRevert(anyInt());
		autoRevert.start();
		autoRevert.onTimer(next()); inOrder.verify(ideNotifications).onTimeTillRevert(anyInt());
	}

	@Test public void revertsChanges_WhenReceivedEnoughTimeUpdates() {
		autoRevert.start();

		autoRevert.onTimer(next());
		autoRevert.onTimer(next());
		autoRevert.onTimer(next());
		autoRevert.onTimer(next());

		verify(ideActions, times(2)).revertCurrentChangeList();
		verifyNoMoreInteractions(ideActions);
		verify(ideNotifications, times(2)).onChangesRevert();
	}

	@Test public void doesNotRevertChanges_WhenStopped() {
		autoRevert.start();
		autoRevert.onTimer(next());
		autoRevert.stop();
		autoRevert.onTimer(next());

		verify(ideNotifications).onAutoRevertStarted(anyInt());
		verify(ideNotifications).onAutoRevertStopped();
		verifyZeroInteractions(ideActions);
	}

	@Test public void resetsTimeTillRevert_WhenStopped() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.start();
		autoRevert.onTimer(next()); inOrder.verify(ideNotifications).onTimeTillRevert(eq(2));
		autoRevert.stop();
		autoRevert.start();
		autoRevert.onTimer(next()); inOrder.verify(ideNotifications).onTimeTillRevert(eq(2));
		autoRevert.onTimer(next()); inOrder.verify(ideNotifications).onTimeTillRevert(eq(1));
	}

	@Test public void resetsTimeTillRevert_WhenCommitted() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.start();
		autoRevert.onTimer(next());  inOrder.verify(ideNotifications).onTimeTillRevert(eq(2));
		autoRevert.onAllFilesCommitted(); inOrder.verify(ideNotifications).onCommit(secondsTillRevert);
		autoRevert.onTimer(next());  inOrder.verify(ideNotifications).onTimeTillRevert(eq(2));
		autoRevert.onTimer(next());  inOrder.verify(ideNotifications).onTimeTillRevert(eq(1));
	}

	@Test public void sendsUINotificationOnCommit_OnlyWhenStarted() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.onAllFilesCommitted(); inOrder.verify(ideNotifications, times(0)).onCommit(anyInt());
		autoRevert.start();
		autoRevert.onAllFilesCommitted(); inOrder.verify(ideNotifications).onCommit(anyInt());
	}

	@Test public void appliesRevertTimeOutChange_AfterStart() {
		autoRevert.onSettings(new Settings(1));
		autoRevert.start();
		autoRevert.onTimer(next());
		autoRevert.onTimer(next());

		verify(ideActions, times(2)).revertCurrentChangeList();
		verify(ideNotifications, times(2)).onChangesRevert();
	}

	@Test public void appliesRevertTimeoutChange_AfterEndOfCurrentTimeOut() {
		autoRevert.start();
		autoRevert.onSettings(new Settings(1));
		autoRevert.onTimer(next());
		autoRevert.onTimer(next()); // reverts changes after 2nd time event
		autoRevert.onTimer(next()); // reverts changes after 1st time event
		autoRevert.onTimer(next()); // reverts changes after 1st time event

		verify(ideActions, times(3)).revertCurrentChangeList();
		verify(ideNotifications, times(3)).onChangesRevert();
	}

	@Test public void appliesRevertTimeoutChange_AfterCommit() {
		autoRevert.start();
		autoRevert.onSettings(new Settings(1));
		autoRevert.onTimer(next());
		autoRevert.onAllFilesCommitted();
		autoRevert.onTimer(next()); // reverts changes after 1st time event
		autoRevert.onTimer(next()); // reverts changes after 1st time event
		autoRevert.onTimer(next()); // reverts changes after 1st time event

		verify(ideActions, times(3)).revertCurrentChangeList();
		verify(ideNotifications, times(3)).onChangesRevert();
	}

	@Test public void doesNotSendUIStartupNotification_WhenDisabled() {
		autoRevert.onSettings(new Settings(false, secondsTillRevert, false));
		autoRevert.start();

		verifyZeroInteractions(ideNotifications);
		verifyZeroInteractions(ideActions);
	}

	@Test public void doesNotRevertChanges_WhenDisabled() {
		autoRevert.start();
		autoRevert.onTimer(next());
		autoRevert.onSettings(new Settings(false, 2, false));
		autoRevert.onTimer(next());

		verifyZeroInteractions(ideActions);
	}

	@Before public void setUp() throws Exception {
		secondsSinceStart = 0;
	}

	private int next() {
		return ++secondsSinceStart;
	}
}
