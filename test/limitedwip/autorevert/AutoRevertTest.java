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
package limitedwip.autorevert;

import limitedwip.autorevert.AutoRevert.Settings;
import limitedwip.autorevert.components.IdeAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;


public class AutoRevertTest {
	private static final int secondsTillRevert = 2;

	private final IdeAdapter ideAdapter = mock(IdeAdapter.class);
	private final Settings settings = new Settings(true, secondsTillRevert, true);
	private final AutoRevert autoRevert = new AutoRevert(ideAdapter).init(settings);
	private int secondsSinceStart;


	@Test public void sendsUIStartupNotification() {
		autoRevert.start();
		verify(ideAdapter).onAutoRevertStarted(eq(secondsTillRevert));
	}

	@Test public void sendsUINotificationOnTimer_OnlyWhenStarted() {
		InOrder inOrder = inOrder(ideAdapter);

		autoRevert.onTimer(next()); inOrder.verify(ideAdapter, times(0)).onTimeTillRevert(anyInt());
		autoRevert.start();
		autoRevert.onTimer(next()); inOrder.verify(ideAdapter).onTimeTillRevert(anyInt());
	}

	@Test public void revertsChanges_WhenReceivedEnoughTimeUpdates() {
		autoRevert.start();

		autoRevert.onTimer(next());
		autoRevert.onTimer(next());
		autoRevert.onTimer(next());
		autoRevert.onTimer(next());

		verify(ideAdapter, times(2)).revertCurrentChangeList();
		verify(ideAdapter, times(2)).onChangesRevert();
	}

	@Test public void doesNotRevertChanges_WhenStopped() {
		autoRevert.start();
		autoRevert.onTimer(next());
		autoRevert.stop();
		autoRevert.onTimer(next());

		verify(ideAdapter).onAutoRevertStarted(anyInt());
		verify(ideAdapter).onAutoRevertStopped();
		verify(ideAdapter, never()).revertCurrentChangeList();
	}

	@Test public void resetsTimeTillRevert_WhenStopped() {
		InOrder inOrder = inOrder(ideAdapter);

		autoRevert.start();
		autoRevert.onTimer(next()); inOrder.verify(ideAdapter).onTimeTillRevert(eq(2));
		autoRevert.stop();
		autoRevert.start();
		autoRevert.onTimer(next()); inOrder.verify(ideAdapter).onTimeTillRevert(eq(2));
		autoRevert.onTimer(next()); inOrder.verify(ideAdapter).onTimeTillRevert(eq(1));
	}

	@Test public void resetsTimeTillRevert_WhenCommitted() {
		InOrder inOrder = inOrder(ideAdapter);

		autoRevert.start();
		autoRevert.onTimer(next());  inOrder.verify(ideAdapter).onTimeTillRevert(eq(2));
		autoRevert.onAllFilesCommitted(); inOrder.verify(ideAdapter).onCommit(secondsTillRevert);
		autoRevert.onTimer(next());  inOrder.verify(ideAdapter).onTimeTillRevert(eq(2));
		autoRevert.onTimer(next());  inOrder.verify(ideAdapter).onTimeTillRevert(eq(1));
	}

	@Test public void sendsUINotificationOnCommit_OnlyWhenStarted() {
		InOrder inOrder = inOrder(ideAdapter);

		autoRevert.onAllFilesCommitted(); inOrder.verify(ideAdapter, times(0)).onCommit(anyInt());
		autoRevert.start();
		autoRevert.onAllFilesCommitted(); inOrder.verify(ideAdapter).onCommit(anyInt());
	}

	@Test public void appliesRevertTimeOutChange_AfterStart() {
		autoRevert.onSettings(new Settings(1));
		autoRevert.start();
		autoRevert.onTimer(next());
		autoRevert.onTimer(next());

		verify(ideAdapter, times(2)).revertCurrentChangeList();
		verify(ideAdapter, times(2)).onChangesRevert();
	}

	@Test public void appliesRevertTimeoutChange_AfterEndOfCurrentTimeOut() {
		autoRevert.start();
		autoRevert.onSettings(new Settings(1));
		autoRevert.onTimer(next());
		autoRevert.onTimer(next()); // reverts changes after 2nd time event
		autoRevert.onTimer(next()); // reverts changes after 1st time event
		autoRevert.onTimer(next()); // reverts changes after 1st time event

		verify(ideAdapter, times(3)).revertCurrentChangeList();
		verify(ideAdapter, times(3)).onChangesRevert();
	}

	@Test public void appliesRevertTimeoutChange_AfterCommit() {
		autoRevert.start();
		autoRevert.onSettings(new Settings(1));
		autoRevert.onTimer(next());
		autoRevert.onAllFilesCommitted();
		autoRevert.onTimer(next()); // reverts changes after 1st time event
		autoRevert.onTimer(next()); // reverts changes after 1st time event
		autoRevert.onTimer(next()); // reverts changes after 1st time event

		verify(ideAdapter, times(3)).revertCurrentChangeList();
		verify(ideAdapter, times(3)).onChangesRevert();
	}

	@Test public void doesNotSendUIStartupNotification_WhenDisabled() {
		Settings disabledSettings = new Settings(false, secondsTillRevert, false);
		autoRevert.onSettings(disabledSettings);
		autoRevert.start();

		verify(ideAdapter).onSettingsUpdate(settings);
		verify(ideAdapter).onSettingsUpdate(disabledSettings);
		verifyNoMoreInteractions(ideAdapter);
	}

	@Test public void doesNotRevertChanges_WhenDisabled() {
		autoRevert.start();
		autoRevert.onTimer(next());
		autoRevert.onSettings(new Settings(false, 2, false));
		autoRevert.onTimer(next());

		verify(ideAdapter, never()).revertCurrentChangeList();
	}

	@Before public void setUp() throws Exception {
		secondsSinceStart = 0;
	}

	private int next() {
		return ++secondsSinceStart;
	}
}
