package ru.autorevert;

import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * User: dima
 * Date: 08/06/2012
 */
public class AutoRevertTest {
	private final IdeNotifications ideNotifications = mock(IdeNotifications.class);
	private final IdeActions ideActions = mock(IdeActions.class);
	private final Model model = new Model(ideNotifications, ideActions, 2);

	@Test public void whenStarted_ShouldSendNotificationToUI() {
		model.start();

		verify(ideNotifications).onAutoRevertStarted();
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenStarted_OnEachTimeEvent_ShouldSentNotificationToUI() {
		model.onTimer();
		model.start();
		model.onTimer();
		model.onTimer();

		verify(ideNotifications, times(2)).onTimer();
	}

	@Test public void whenStarted_And_ReceivesEnoughTimeUpdates_shouldRevertCurrentChangeList() {
		model.start();
		model.onTimer();
		model.onTimer();
		model.onTimer();
		model.onTimer();

		verify(ideNotifications).onAutoRevertStarted();
		verify(ideActions, times(2)).revertCurrentChangeList();
		verifyNoMoreInteractions(ideActions);
	}

	@Test public void whenStartedAndStopped_should_NOT_RevertOnNextTimeout() {
		model.start();
		model.onTimer();
		model.stop();
		model.onTimer();

		verify(ideNotifications).onAutoRevertStarted();
		verify(ideNotifications).onAutoRevertStopped();
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenDetectsCommit_should_NOT_RevertOnNextTimeout() {
		model.start();
		model.onTimer();
		model.onCommit();
		model.onTimer();

		verify(ideNotifications).onAutoRevertStarted();
		verify(ideNotifications).onTimerReset();
		verifyZeroInteractions(ideActions);
	}
}
