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
import limitedwip.watchdog.ChangeSizeWatchdog;

public class WatchdogComponent extends AbstractProjectComponent {
	private ChangeSizeWatchdog changeSizeWatchdog;
	private TimerComponent timer;


	public WatchdogComponent(Project project) {
		super(project);
		timer = ApplicationManager.getApplication().getComponent(TimerComponent.class);
	}

	@Override public void projectOpened() {
		LimitedWIPSettings settings = ServiceManager.getService(LimitedWIPSettings.class);
		IdeNotifications ideNotifications = new IdeNotifications(myProject, convert(settings));
		IdeActions ideActions = new IdeActions(myProject);
		changeSizeWatchdog = new ChangeSizeWatchdog(ideNotifications, ideActions).init(convert(settings));

		timer.addListener(new TimerComponent.Listener() {
			@Override public void onUpdate(final int seconds) {
				ApplicationManager.getApplication().invokeLater(new Runnable() {
					@Override public void run() {
						changeSizeWatchdog.onTimer(seconds);
					}
				}, ModalityState.any());
			}
		}, myProject);

		LimitedWipConfigurable.registerSettingsListener(myProject, new LimitedWipConfigurable.Listener() {
			@Override public void onSettingsUpdate(LimitedWIPSettings settings) {
				changeSizeWatchdog.onSettings(convert(settings));
			}
		});

		LimitedWipCheckin.registerListener(myProject, new LimitedWipCheckin.Listener() {
			@Override public void onSuccessfulCheckin(boolean allFileAreCommitted) {
				changeSizeWatchdog.onCommit();
			}
		});
	}

    public void toggleSkipNotificationsUntilCommit() {
        changeSizeWatchdog.toggleSkipNotificationsUntilCommit();
    }

	public void skipNotificationsUntilCommit(boolean value) {
		changeSizeWatchdog.skipNotificationsUntilCommit(value);
	}

	private static ChangeSizeWatchdog.Settings convert(LimitedWIPSettings settings) {
		return new ChangeSizeWatchdog.Settings(
				settings.watchdogEnabled,
				settings.maxLinesInChange,
				settings.notificationIntervalInSeconds(),
				settings.showRemainingChangesInToolbar
		);
	}
}
