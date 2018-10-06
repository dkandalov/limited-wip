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
package limitedwip.watchdog.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import limitedwip.common.LimitedWipCheckin;
import limitedwip.common.TimerComponent;
import limitedwip.common.settings.LimitedWIPSettings;
import limitedwip.common.settings.LimitedWipConfigurable;
import limitedwip.watchdog.Watchdog;

public class WatchdogComponent extends AbstractProjectComponent {
	private Watchdog watchdog;
	private TimerComponent timer;
	private IdeAdapter ideAdapter;


	public WatchdogComponent(Project project) {
		super(project);
		timer = ApplicationManager.getApplication().getComponent(TimerComponent.class);
	}

	@Override public void projectOpened() {
		LimitedWIPSettings settings = ServiceManager.getService(LimitedWIPSettings.class);
		final ChangeSizeCalculator changeSizeCalculator = new ChangeSizeCalculator(myProject);
		ideAdapter = new IdeAdapter(myProject, changeSizeCalculator);
		watchdog = new Watchdog(ideAdapter).init(convert(settings));

		timer.addListener(new TimerComponent.Listener() {
			@Override public void onUpdate(final int seconds) {
				ApplicationManager.getApplication().invokeLater(new Runnable() {
					@Override public void run() {
						// Project can be closed (disposed) during handover between timer thread and EDT.
						if (myProject.isDisposed()) return;
						watchdog.onTimer(seconds);
						changeSizeCalculator.onTimer();
					}
				}, ModalityState.any());
			}
		}, myProject);

		LimitedWipConfigurable.registerSettingsListener(myProject, new LimitedWipConfigurable.Listener() {
			@Override public void onSettingsUpdate(LimitedWIPSettings settings) {
				watchdog.onSettings(convert(settings));
			}
		});

		LimitedWipCheckin.Companion.registerListener(myProject, new LimitedWipCheckin.Listener() {
			@Override public void onSuccessfulCheckin(boolean allFileAreCommitted) {
				watchdog.onCommit();
			}
		});
	}

	public int currentChangeListSize() {
		return ideAdapter.currentChangeListSizeInLines().value;
	}

	public void toggleSkipNotificationsUntilCommit() {
        watchdog.toggleSkipNotificationsUntilCommit();
    }

	public void skipNotificationsUntilCommit(boolean value) {
		watchdog.skipNotificationsUntilCommit(value);
	}

	private static Watchdog.Settings convert(LimitedWIPSettings settings) {
		return new Watchdog.Settings(
				settings.watchdogEnabled,
				settings.maxLinesInChange,
				settings.notificationIntervalInSeconds(),
				settings.showRemainingChangesInToolbar
		);
	}
}
