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
package limitedwip.ui.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.Range;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "LimitedWIPSettings", storages = {@Storage(id = "other", file = "$APP_CONFIG$/limitedwip.ui.settings.xml")})
public class Settings implements PersistentStateComponent<Settings>  {
	public static final Range<Integer> minutesToRevertRange = new Range<Integer>(1, 99);
	public static final Range<Integer> changedLinesRange = new Range<Integer>(1, 999);
	public static final Range<Integer> notificationIntervalRange = new Range<Integer>(1, 99);

	public boolean autoRevertEnabled = false;
	public int minutesTillRevert = 2;
	public boolean showTimerInToolbar = true;

	public boolean watchdogEnabled = true;
	public int maxLinesInChange = 80;
	public int notificationIntervalInMinutes = 1;
	public boolean disableCommitsAboveThreshold = false;
	public boolean showRemainingInToolbar = true;


	public int secondsTillRevert() {
		return minutesTillRevert * 60;
	}

	public int notificationIntervalInSeconds() {
		return notificationIntervalInMinutes * 60;
	}

	@Override public Settings getState() {
		return this;
	}

	@Override public void loadState(Settings state) {
		XmlSerializerUtil.copyBean(state, this);
	}

	@SuppressWarnings("RedundantIfStatement") @Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Settings settings = (Settings) o;

		if (autoRevertEnabled != settings.autoRevertEnabled) return false;
		if (disableCommitsAboveThreshold != settings.disableCommitsAboveThreshold) return false;
		if (maxLinesInChange != settings.maxLinesInChange) return false;
		if (minutesTillRevert != settings.minutesTillRevert) return false;
		if (notificationIntervalInMinutes != settings.notificationIntervalInMinutes) return false;
		if (showRemainingInToolbar != settings.showRemainingInToolbar) return false;
		if (showTimerInToolbar != settings.showTimerInToolbar) return false;
		if (watchdogEnabled != settings.watchdogEnabled) return false;

		return true;
	}

	@Override public int hashCode() {
		int result = (autoRevertEnabled ? 1 : 0);
		result = 31 * result + minutesTillRevert;
		result = 31 * result + (showTimerInToolbar ? 1 : 0);
		result = 31 * result + (watchdogEnabled ? 1 : 0);
		result = 31 * result + maxLinesInChange;
		result = 31 * result + notificationIntervalInMinutes;
		result = 31 * result + (disableCommitsAboveThreshold ? 1 : 0);
		result = 31 * result + (showRemainingInToolbar ? 1 : 0);
		return result;
	}

	@Override public String toString() {
		return "Settings{" +
				"autoRevertEnabled=" + autoRevertEnabled +
				", minutesTillRevert=" + minutesTillRevert +
				", showTimerInToolbar=" + showTimerInToolbar +
				", watchdogEnabled=" + watchdogEnabled +
				", maxLinesInChange=" + maxLinesInChange +
				", notificationIntervalInMinutes=" + notificationIntervalInMinutes +
				", disableCommitsAboveThreshold=" + disableCommitsAboveThreshold +
				", showRemainingInToolbar=" + showRemainingInToolbar +
				'}';
	}

	public interface Listener {
		void onSettings(Settings settings);
	}
}
