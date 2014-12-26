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
package limitedwip.components;

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class TimerEventsSourceAppComponent implements ApplicationComponent {
	private static final int ONE_SECOND = 1000;

	private final Timer timer = new Timer();
	private final List<Listener> listeners = new ArrayList<Listener>();

	@Override public void initComponent() {
		timer.schedule(new TimerTask() {
			@Override public void run() {
				for (Listener listener : listeners) {
					listener.onTimerEvent();
				}
			}
		}, 0, ONE_SECOND);
	}

	@Override public void disposeComponent() {
		timer.cancel();
		timer.purge();
	}

	@NotNull @Override public String getComponentName() {
		return "LimitedWIP-TimeEventsSource";
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public interface Listener {
		void onTimerEvent();
	}
}
