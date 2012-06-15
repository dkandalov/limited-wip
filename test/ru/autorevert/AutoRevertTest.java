package ru.autorevert;

import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

/**
 * User: dima
 * Date: 08/06/2012
 */
public class AutoRevertTest {
	private static final int TIME_EVENTS_TILL_REVERT = 2;

	private final IdeNotifications ideNotifications = mock(IdeNotifications.class);
	private final IdeActions ideActions = mock(IdeActions.class);
	private final Model model = new Model(ideNotifications, ideActions, TIME_EVENTS_TILL_REVERT);

	@Test public void whenStarted_ShouldSendNotificationToUI() {
		model.start();

		verify(ideNotifications).onAutoRevertStarted(TIME_EVENTS_TILL_REVERT);
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenStarted_OnEachTimeEvent_ShouldSentNotificationToUI() {
		InOrder inOrder = inOrder(ideNotifications);

		model.onTimer();
		model.start();
		model.onTimer(); inOrder.verify(ideNotifications).onTimer(eq(2));
		model.onTimer(); inOrder.verify(ideNotifications).onTimer(eq(1));
		model.onTimer(); inOrder.verify(ideNotifications).onTimer(eq(2));
	}

	@Test public void whenStarted_And_ReceivesEnoughTimeUpdates_shouldRevertCurrentChangeList() {
		model.start();
		model.onTimer();
		model.onTimer();
		model.onTimer();
		model.onTimer();

		verify(ideActions, times(2)).revertCurrentChangeList();
		verifyNoMoreInteractions(ideActions);
	}

	@Test public void whenStopped_should_NOT_RevertOnNextTimeout() {
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
		model.onTimer(); inOrder.verify(ideNotifications).onTimer(2);
		model.stop();
		model.start();
		model.onTimer(); inOrder.verify(ideNotifications).onTimer(2);
		model.onTimer(); inOrder.verify(ideNotifications).onTimer(1);
	}

	@Test public void whenDetectsCommit_should_NOT_RevertOnNextTimeout() {
		model.start();
		model.onTimer();
		model.onCommit();
		model.onTimer();

		verify(ideNotifications).onAutoRevertStarted(anyInt());
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenDetectsCommit_should_ResetTimeLeftTillRevert_And_NotifyUI() {
		InOrder inOrder = inOrder(ideNotifications);

		model.start();
		model.onTimer();  inOrder.verify(ideNotifications).onTimer(2);
		model.onCommit(); inOrder.verify(ideNotifications).onCommit(TIME_EVENTS_TILL_REVERT);
		model.onTimer();  inOrder.verify(ideNotifications).onTimer(2);
		model.onTimer();  inOrder.verify(ideNotifications).onTimer(1);
	}

	@Test public void whenTimeTillRevertChanges_should_ApplyItAfterStart() {
		model.onNewSettings(1);
		model.start();
		model.onTimer();
		model.onTimer();

		verify(ideActions, times(2)).revertCurrentChangeList();
	}

	@Test public void whenTimeTillRevertChangesAfterStart_should_ApplyItOnlyAfterNext_Revert() {
		model.start();
		model.onNewSettings(1);
		model.onTimer();
		model.onTimer(); // reverts changes after 2nd time event
		model.onTimer(); // reverts changes after 1st time event
		model.onTimer(); // reverts changes after 1st time event

		verify(ideActions, times(3)).revertCurrentChangeList();
	}

	@Test public void whenTimeTillRevertChangesAfterStart_should_ApplyItOnlyAfterNext_Commit() {
		model.start();
		model.onNewSettings(1);
		model.onTimer();
		model.onCommit();
		model.onTimer(); // reverts changes after 1st time event
		model.onTimer(); // reverts changes after 1st time event
		model.onTimer(); // reverts changes after 1st time event

		verify(ideActions, times(3)).revertCurrentChangeList();
	}
}
