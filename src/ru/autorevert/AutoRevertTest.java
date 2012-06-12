package ru.autorevert;

import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * User: dima
 * Date: 08/06/2012
 */
public class AutoRevertTest {
	private final IDEService ideService = mock(IDEService.class);
	private final Model model = new Model(ideService, 2);

	@Test public void whenStarted_ShouldSendNotificationToUI() {
		model.start();

		verify(ideService).autoRevertStarted();
		verifyNoMoreInteractions(ideService);
	}

	@Test public void whenStarted_OnEachTimeEvent_ShouldSentNotificationToUI() {
		model.onTimer();
		model.start();
		model.onTimer();

		verify(ideService, times(1)).onTimer();
		verifyNoMoreInteractions(ideService);
	}

	@Test public void whenStarted_AndReceivesEnoughTimeUpdates_shouldRevertCurrentChangeList() {
		IDEService ideService = mock(IDEService.class);
		Model model = new Model(ideService, 2);

		model.start();
		model.onTimer();
		model.onTimer();
		model.onTimer();
		model.onTimer();

		verify(ideService).autoRevertStarted();
		verify(ideService, times(2)).doRevertCurrentChangeList();
		verifyNoMoreInteractions(ideService);
	}

	@Test public void whenStartedAndStopped_should_NOT_RevertOnNextTimeout() {
		IDEService ideService = mock(IDEService.class);
		Model model = new Model(ideService, 2);

		model.start();
		model.onTimer();
		model.stop();
		model.onTimer();

		verify(ideService).autoRevertStarted();
		verify(ideService).autoRevertStopped();
		verifyNoMoreInteractions(ideService);
	}

	@Test public void whenDetectsCommit_should_NOT_RevertOnNextTimeout() {
		IDEService ideService = mock(IDEService.class);
		Model model = new Model(ideService, 2);

		model.start();
		model.onTimer();
		model.onCommit();
		model.onTimer();

		verify(ideService).autoRevertStarted();
		verify(ideService).timerWasReset();
		verifyNoMoreInteractions(ideService);
	}

	private static class Model {

		private final IDEService ideService;
		private final int timeEventsTillRevert;

		private boolean started = false;
		private int timeEventCounter;

		public Model(IDEService ideService, int timeEventsTillRevert) {
			this.ideService = ideService;
			this.timeEventsTillRevert = timeEventsTillRevert;
		}

		public void start() {
			started = true;
			ideService.autoRevertStarted();
		}

		public void stop() {
			started = false;
			ideService.autoRevertStopped();
		}

		public void onTimer() {
			if (!started) return;

			timeEventCounter++;
			if (timeEventCounter >= timeEventsTillRevert) {
				timeEventCounter = 0;
				ideService.doRevertCurrentChangeList();
			}
		}

		public void onCommit() {
			timeEventCounter = 0;
			ideService.timerWasReset();
		}
	}

	private static class IDEService {
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
