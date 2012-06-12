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
		Model model = new Model(ideService, 3);

		model.start();

		verify(ideService).autoRevertStarted();
	}

	@Test public void whenStarted_AndReceivedEnoughTimeUpdates_shouldRevertCurrentChangeList() {
		IDEService ideService = mock(IDEService.class);
		Model model = new Model(ideService, 3);

		model.start();
		model.onTimer();
		model.onTimer();
		model.onTimer();

		verify(ideService).autoRevertStarted();
		verify(ideService).revertCurrentChangeList();
	}

	@Test public void whenStartedAndStopped_should_NOT_RevertCurrentChangeList() {
		IDEService ideService = mock(IDEService.class);
		Model model = new Model(ideService, 3);

		model.start();
		model.onTimer();
		model.onTimer();
		model.stop();
		model.onTimer();

		verify(ideService).autoRevertStarted();
		verify(ideService).autoRevertStopped();
		verifyNoMoreInteractions(ideService);
	}

	private static class Model {

		private final IDEService ideService;
		private final int timeEventsTillRevert;
		private int timeEventCounter;

		public Model(IDEService ideService, int timeEventsTillRevert) {
			this.ideService = ideService;
			this.timeEventsTillRevert = timeEventsTillRevert;
		}

		public void start() {
			ideService.autoRevertStarted();
		}

		public void onTimer() {
			timeEventCounter++;
			if (timeEventCounter >= timeEventsTillRevert) {
				timeEventCounter = 0;
				ideService.revertCurrentChangeList();
			}
		}
	}

	private static class IDEService {
		public void autoRevertStarted() {
			// TODO implement

		}

		public void revertCurrentChangeList() {
			// TODO implement

		}
	}
}
