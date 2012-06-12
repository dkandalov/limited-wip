package ru.autorevert;

import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * User: dima
 * Date: 08/06/2012
 */
public class AutoRevertTest {
	private final IdeNotification ideNotification = mock(IdeNotification.class);
	private final Model model = new Model(ideNotification, 2);

	@Test public void whenStarted_ShouldSendNotificationToUI() {
		model.start();

		verify(ideNotification).autoRevertStarted();
		verifyNoMoreInteractions(ideNotification);
	}

	@Test public void whenStarted_OnEachTimeEvent_ShouldSentNotificationToUI() {
		model.onTimer();
		model.start();
		model.onTimer();

		verify(ideNotification, times(1)).onTimer();
		verifyNoMoreInteractions(ideNotification);
	}

	@Test public void whenStarted_AndReceivesEnoughTimeUpdates_shouldRevertCurrentChangeList() {
		model.start();
		model.onTimer();
		model.onTimer();
		model.onTimer();
		model.onTimer();

		verify(ideNotification).autoRevertStarted();
		verify(ideNotification, times(2)).doRevertCurrentChangeList();
		verifyNoMoreInteractions(ideNotification);
	}

	@Test public void whenStartedAndStopped_should_NOT_RevertOnNextTimeout() {
		model.start();
		model.onTimer();
		model.stop();
		model.onTimer();

		verify(ideNotification).autoRevertStarted();
		verify(ideNotification).autoRevertStopped();
		verifyNoMoreInteractions(ideNotification);
	}

	@Test public void whenDetectsCommit_should_NOT_RevertOnNextTimeout() {
		model.start();
		model.onTimer();
		model.onCommit();
		model.onTimer();

		verify(ideNotification).autoRevertStarted();
		verify(ideNotification).timerWasReset();
		verifyNoMoreInteractions(ideNotification);
	}

	private static class Model {
		private final IdeNotification ideNotification;
		private final int timeEventsTillRevert;

		private boolean started = false;
		private int timeEventCounter;

		public Model(IdeNotification ideNotification, int timeEventsTillRevert) {
			this.ideNotification = ideNotification;
			this.timeEventsTillRevert = timeEventsTillRevert;
		}

		public void start() {
			started = true;
			ideNotification.autoRevertStarted();
		}

		public void stop() {
			started = false;
			ideNotification.autoRevertStopped();
		}

		public void onTimer() {
			if (!started) return;

			timeEventCounter++;
			if (timeEventCounter >= timeEventsTillRevert) {
				timeEventCounter = 0;
				ideNotification.doRevertCurrentChangeList();
			}
		}

		public void onCommit() {
			timeEventCounter = 0;
			ideNotification.timerWasReset();
		}
	}

	private static class IdeNotification {
		public void autoRevertStarted() {
			// TODO implement

		}

		public void doRevertCurrentChangeList() {
			// TODO implement

		}

		public void autoRevertStopped() {
			// TODO implement

		}

		public void timerWasReset() {
			// TODO implement

		}

		public void onTimer() {
			// TODO implement

		}
	}
}
