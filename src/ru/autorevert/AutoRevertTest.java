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
		Model model = new Model(ideService);
		model.start();

		verify(ideService).autoRevertStarted();
	}

	private static class Model {

		public Model(IDEService ideService) {
			// TODO implement

		}

		public void start() {
			// TODO implement

		}
	}

	private static class IDEService {
		public void autoRevertStarted() {
			// TODO implement

		}
	}
}
