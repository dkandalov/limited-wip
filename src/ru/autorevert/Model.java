package ru.autorevert;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class Model {
	private final IdeNotification ideNotification;
	private final IdeActions ideActions;
	private final int timeEventsTillRevert;

	private boolean started = false;
	private int timeEventCounter;

	public Model(IdeNotification ideNotification, IdeActions ideActions, int timeEventsTillRevert) {
		this.ideNotification = ideNotification;
		this.ideActions = ideActions;
		this.timeEventsTillRevert = timeEventsTillRevert;
	}

	public void start() {
		started = true;
		ideNotification.onAutoRevertStarted();
	}

	public void stop() {
		started = false;
		ideNotification.onAutoRevertStopped();
	}

	public boolean isStarted() {
		return started;
	}

	public void onTimer() {
		if (!started) return;

		timeEventCounter++;
		ideNotification.onTimer();

		if (timeEventCounter >= timeEventsTillRevert) {
			timeEventCounter = 0;
			ideActions.revertCurrentChangeList();
		}
	}

	public void onCommit() {
		timeEventCounter = 0;
		ideNotification.onTimerReset();
	}
}
