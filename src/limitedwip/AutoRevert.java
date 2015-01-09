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

	private boolean started = false;
	private int newSecondsTillRevert;
	private int secondsTillRevert;
	private int startSeconds;
	private boolean disabled;

	public AutoRevert(IdeNotifications ideNotifications, IdeActions ideActions, int secondsTillRevert) {
		this.ideNotifications = ideNotifications;
		this.ideActions = ideActions;
		this.secondsTillRevert = secondsTillRevert;
		this.newSecondsTillRevert = secondsTillRevert;
	}

	public synchronized void start() {
		if (disabled) return;

		started = true;
		startSeconds = -1;
		applyNewSettings();

		ideNotifications.onAutoRevertStarted(secondsTillRevert);
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

		ideNotifications.onTimeTillRevert(secondsTillRevert - secondsPassed + 1);

		if (secondsPassed >= secondsTillRevert) {
			startSeconds = -1;
			applyNewSettings();
			ideActions.revertCurrentChangeList();
		}
	}

	public synchronized void onCommit() {
		if (!started) return;

		startSeconds = -1;
		applyNewSettings();
		ideNotifications.onCommit(secondsTillRevert);
	}

	public synchronized void on(Settings settings) {
		this.newSecondsTillRevert = settings.secondsTillRevert;
		this.disabled = !settings.enabled;
		if (started && disabled) {
			stop();
		}
	}

	private void applyNewSettings() {
		if (secondsTillRevert != newSecondsTillRevert) {
			secondsTillRevert = newSecondsTillRevert;
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
