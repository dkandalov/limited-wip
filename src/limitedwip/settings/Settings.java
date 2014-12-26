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
package limitedwip.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "AutoRevertSettings", storages = {@Storage(id = "other", file = "$APP_CONFIG$/autorevert.settings.xml")})
public class Settings implements PersistentStateComponent<Settings>  {
	public static final Integer MIN_MINUTES_TO_REVERT = 1;
	public static final Integer MAX_MINUTES_TO_REVERT = 99;

	public int minutesTillRevert = 2;
	public boolean showTimerInToolbar = true;
	public boolean disableCommitsWithErrors = false;


	public int secondsTillRevert() {
		return minutesTillRevert * 60;
	}

	@Override public Settings getState() {
		return this;
	}

	@Override public void loadState(Settings state) {
		XmlSerializerUtil.copyBean(state, this);
	}

	@SuppressWarnings("RedundantIfStatement") @Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Settings settings = (Settings) o;

		if (minutesTillRevert != settings.minutesTillRevert) return false;
		if (showTimerInToolbar != settings.showTimerInToolbar) return false;
		if (disableCommitsWithErrors != settings.disableCommitsWithErrors) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = minutesTillRevert;
		result = 31 * result + (showTimerInToolbar ? 1 : 0);
		result = 31 * result + (disableCommitsWithErrors ? 1 : 0);
		return result;
	}

	@Override public String toString() {
		return "Settings{" +
				"minutesTillRevert=" + minutesTillRevert +
				", showTimerInToolbar=" + showTimerInToolbar +
				", disableCommitsWithErrors=" + disableCommitsWithErrors +
				'}';
	}

	public interface Listener {
		void onNewSettings(Settings settings);
	}
}
