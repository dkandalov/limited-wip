package ru.autorevert;

import org.junit.Test;

/**
 * User: dima
 * Date: 08/06/2012
 */
public class AutoRevertTest {

	@Test public void whenStartedModelShouldSendNotificationToUI() {

		IDEService ideService = new IDEService();
		Model model = new Model(ideService);
		model.start();
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
	}
}
