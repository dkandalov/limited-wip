package ru.autorevert;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class Model {
	private boolean started = false;

	public synchronized void start() {
		started = true;
	}

	public synchronized void stop() {
		started = false;
	}
}
