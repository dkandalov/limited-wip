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
package limitedwip;


public class AutoRevert {
	private final IdeNotifications ideNotifications;
	private final IdeActions ideActions;

	private Settings settings;
	private boolean started = false;
	private int startSeconds;
	private int remainingSeconds;


	public AutoRevert(IdeNotifications ideNotifications, IdeActions ideActions, Settings settings) {
		this.ideNotifications = ideNotifications;
		this.ideActions = ideActions;
		this.settings = settings;
	}

	public synchronized void start() {
		if (!settings.enabled) return;

		started = true;
		startSeconds = -1;
		applyNewSettings();

		ideNotifications.onAutoRevertStarted(remainingSeconds);
	}

	public synchronized void stop() {
		started = false;
		ideNotifications.onAutoRevertStopped();
	}

	public synchronized boolean isStarted() {
		return started;
	}

	public synchronized void onTimer(int seconds) {
		if (!started) return;

		if (startSeconds == -1) {
			startSeconds = seconds - 1;
		}
		int secondsPassed = seconds - startSeconds;

		ideNotifications.onTimeTillRevert(remainingSeconds - secondsPassed + 1);

		if (secondsPassed >= remainingSeconds) {
			startSeconds = -1;
			applyNewSettings();
			ideActions.revertCurrentChangeList();
		}
	}

	public synchronized void onAllFilesCommitted() {
		if (!started) return;

		startSeconds = -1;
		applyNewSettings();
		ideNotifications.onCommit(remainingSeconds);
	}

	public synchronized void onSettings(Settings settings) {
		this.settings = settings;
		if (started && !settings.enabled) {
			stop();
		}
	}

	private void applyNewSettings() {
		if (remainingSeconds != settings.secondsTillRevert) {
			remainingSeconds = settings.secondsTillRevert;
		}
	}

	public static class Settings {
		public final boolean enabled;
		public final int secondsTillRevert;

		public Settings(int secondsTillRevert) {
			this(true, secondsTillRevert);
		}

		public Settings(boolean enabled, int secondsTillRevert) {
			this.enabled = enabled;
			this.secondsTillRevert = secondsTillRevert;
		}
	}
}
