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

	public AutoRevert(IdeNotifications ideNotifications, IdeActions ideActions, int secondsTillRevert) {
		this.ideNotifications = ideNotifications;
		this.ideActions = ideActions;
		this.secondsTillRevert = secondsTillRevert;
		this.newSecondsTillRevert = secondsTillRevert;
	}

	public synchronized void start() {
		started = true;
		startSeconds = -1;
		applySettingsForSecondsTillRevert();

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
			applySettingsForSecondsTillRevert();
			ideActions.revertCurrentChangeList();
		}
	}

	public synchronized void onCommit() {
		if (!started) return;

		startSeconds = -1;
		applySettingsForSecondsTillRevert();
		ideNotifications.onCommit(secondsTillRevert);
	}

	public synchronized void on(SettingsUpdate settingsUpdate) {
		this.newSecondsTillRevert = settingsUpdate.secondsTillRevert;
		if (!settingsUpdate.enabled && started) {
			stop();
		}
	}

	private void applySettingsForSecondsTillRevert() {
		if (secondsTillRevert != newSecondsTillRevert) {
			secondsTillRevert = newSecondsTillRevert;
		}
	}

	public static class SettingsUpdate {
		public final boolean enabled;
		public final int secondsTillRevert;

		public SettingsUpdate(int secondsTillRevert) {
			this(true, secondsTillRevert);
		}

		public SettingsUpdate(boolean enabled, int secondsTillRevert) {
			this.enabled = enabled;
			this.secondsTillRevert = secondsTillRevert;
		}
	}
}
