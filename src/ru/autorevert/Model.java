package ru.autorevert;

/**
 * TODO should reset timer on user revert?
 *
 * User: dima
 * Date: 10/06/2012
 */
public class Model {
	private final IdeNotifications ideNotifications;
	private final IdeActions ideActions;
	private final int timeEventsTillRevert;

	private boolean started = false;
	private int timeEventCounter;

	public Model(IdeNotifications ideNotifications, IdeActions ideActions, int timeEventsTillRevert) {
		this.ideNotifications = ideNotifications;
		this.ideActions = ideActions;
		this.timeEventsTillRevert = timeEventsTillRevert;
	}

	public synchronized void start() {
		started = true;
		timeEventCounter = 0;
		ideNotifications.onAutoRevertStarted(timeEventsTillRevert);
	}

	public synchronized void stop() {
		started = false;
		ideNotifications.onAutoRevertStopped();
	}

	public synchronized boolean isStarted() {
		return started;
	}

	public synchronized void onTimer() {
		if (!started) return;

		timeEventCounter++;
		ideNotifications.onTimer(timeEventsTillRevert - timeEventCounter + 1);

		if (timeEventCounter >= timeEventsTillRevert) {
			timeEventCounter = 0;
			ideActions.revertCurrentChangeList();
		}
	}

	public synchronized void onCommit() {
		timeEventCounter = 0;
		ideNotifications.onCommit(timeEventsTillRevert);
	}
}
