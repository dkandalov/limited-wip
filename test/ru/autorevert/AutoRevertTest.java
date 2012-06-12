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

		verify(ideNotifications).onAutoRevertStarted();
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenStarted_OnEachTimeEvent_ShouldSentNotificationToUI() {
		InOrder inOrder = inOrder(ideNotifications);

		model.onTimer();
		model.start();
		model.onTimer();
		model.onTimer();

		inOrder.verify(ideNotifications).onTimeTillRevert(2);
		inOrder.verify(ideNotifications).onTimeTillRevert(1);
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

		verify(ideNotifications).onAutoRevertStarted();
		verify(ideNotifications).onAutoRevertStopped();
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenStopped_should_ResetTimeLeftTillRevert() {
		InOrder inOrder = inOrder(ideNotifications);

		model.start();
		model.onTimer();
		model.stop();
		model.start();
		model.onTimer();
		model.onTimer();

		inOrder.verify(ideNotifications).onTimeTillRevert(2);
		inOrder.verify(ideNotifications).onTimeTillRevert(2);
		inOrder.verify(ideNotifications).onTimeTillRevert(1);
	}

	@Test public void whenDetectsCommit_should_NOT_RevertOnNextTimeout() {
		model.start();
		model.onTimer();
		model.onCommit();
		model.onTimer();

		verify(ideNotifications).onAutoRevertStarted();
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenDetectsCommit_should_ResetTimeLeftTillRevert() {
		InOrder inOrder = inOrder(ideNotifications);

		model.start();
		model.onTimer();
		model.onCommit();
		model.onTimer();

		inOrder.verify(ideNotifications).onTimeTillRevert(2);
		inOrder.verify(ideNotifications).onTimeTillRevert(2);
		inOrder.verify(ideNotifications).onTimeTillRevert(1);
	}
}
