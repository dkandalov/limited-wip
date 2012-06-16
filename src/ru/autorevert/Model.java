/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.autorevert;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class Model {
	private final IdeNotifications ideNotifications;
	private final IdeActions ideActions;

	private boolean started = false;
	private int newTimeEventTillRevert;
	private int timeEventsTillRevert;
	private int timeEventCounter;

	public Model(IdeNotifications ideNotifications, IdeActions ideActions, int timeEventsTillRevert) {
		this.ideNotifications = ideNotifications;
		this.ideActions = ideActions;
		this.timeEventsTillRevert = timeEventsTillRevert;
		this.newTimeEventTillRevert = timeEventsTillRevert;
	}

	public synchronized void start() {
		started = true;
		timeEventCounter = 0;
		updateTimeEventsTillRevert();

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
			updateTimeEventsTillRevert();
			ideActions.revertCurrentChangeList();
		}
	}

	public synchronized void onCommit() {
		if (!started) return;

		timeEventCounter = 0;
		updateTimeEventsTillRevert();
		ideNotifications.onCommit(timeEventsTillRevert);
	}

	public void onNewSettings(int newTimeEventTillRevert) {
		this.newTimeEventTillRevert = newTimeEventTillRevert;
	}

	private void updateTimeEventsTillRevert() {
		if (timeEventsTillRevert != newTimeEventTillRevert) {
			timeEventsTillRevert = newTimeEventTillRevert;
		}
	}
}
