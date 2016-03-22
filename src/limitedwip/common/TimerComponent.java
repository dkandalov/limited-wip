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
package limitedwip.common;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;


public class TimerComponent implements ApplicationComponent {
	private static final Logger log = Logger.getInstance(TimerComponent.class);
	private static final int oneSecond = 1000;

	// TODO use intellij api
	private final Timer timer = new Timer("LimitedWIP-TimeEvents");
	private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
	private int secondsSinceStart = 0;

	@Override public void initComponent() {
		timer.schedule(new TimerTask() {
			@Override public void run() {
				try {
					secondsSinceStart++;
					for (Listener listener : listeners) {
                        listener.onUpdate(secondsSinceStart);
                    }
				} catch (ProcessCanceledException ignored) {
				} catch (Exception e) {
					log.error(e);
				}
			}
		}, 0, oneSecond);
	}

	@Override public void disposeComponent() {
		timer.cancel();
		timer.purge();
	}

	@NotNull @Override public String getComponentName() {
		return "LimitedWIP-TimeEventsSource";
	}

	public void addListener(final Listener listener, Disposable parentDisposable) {
		listeners.add(listener);
		Disposer.register(parentDisposable, new Disposable() {
			@Override public void dispose() {
				listeners.remove(listener);
			}
		});
	}

	public interface Listener {
		void onUpdate(int seconds);
	}
}
