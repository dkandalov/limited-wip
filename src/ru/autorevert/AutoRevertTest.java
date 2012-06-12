package ru.autorevert;

import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * User: dima
 * Date: 08/06/2012
 */
public class AutoRevertTest {

	@Test public void whenStartedModelShouldSendNotificationToUI() {
		IDEService ideService = mock(IDEService.class);
		Model model = new Model(ideService, 2);

		model.start();

		verify(ideService).autoRevertStarted();
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
		verify(ideService, times(2)).revertCurrentChangeList();
		verifyNoMoreInteractions(ideService);
	}

	@Test public void whenStartedAndStopped_should_NOT_RevertCurrentChangeList() {
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

	@Test public void whenNotifiesCommit_should_WaitForAnotherTimeoutToRevert() {
		IDEService ideService = mock(IDEService.class);
		Model model = new Model(ideService, 2);

		model.start();
		model.onTimer();
		model.onCommit();
		model.onTimer();

		verify(ideService).autoRevertStarted();
		verify(ideService).timerReset();
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
				ideService.revertCurrentChangeList();
			}
		}

		public void onCommit() {
			timeEventCounter = 0;
			ideService.timerReset();
		}
	}

	private static class IDEService {
		public void autoRevertStarted() {
			// TODO implement

		}

		public void revertCurrentChangeList() {
			// TODO implement

		}

		public void autoRevertStopped() {
			// TODO implement

		}

		public void timerReset() {
			// TODO implement

		}
	}
}
