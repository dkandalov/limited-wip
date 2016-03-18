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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import limitedwip.AutoRevert;
import limitedwip.ChangeSizeWatchdog;
import limitedwip.IdeActions;
import limitedwip.IdeNotifications;
import limitedwip.ui.settings.LimitedWIPSettings;

public class LimitedWIPProjectComponent extends AbstractProjectComponent implements LimitedWIPSettings.Listener {
	private ChangeSizeWatchdog changeSizeWatchdog;
	private AutoRevert autoRevert;
	private IdeNotifications ideNotifications;

	private TimerEventsSource.Listener timerListener;


	public LimitedWIPProjectComponent(Project project) {
		super(project);
	}

	@Override public void projectOpened() {
		super.projectOpened();

		LimitedWIPSettings settings = ServiceManager.getService(LimitedWIPSettings.class);
		ideNotifications = new IdeNotifications(myProject, settings);
		IdeActions ideActions = new IdeActions(myProject);
		autoRevert = new AutoRevert(ideNotifications, ideActions, new AutoRevert.Settings(
				settings.autoRevertEnabled,
				settings.secondsTillRevert(),
				settings.notifyOnRevert
		));
		changeSizeWatchdog = new ChangeSizeWatchdog(ideNotifications, ideActions, new ChangeSizeWatchdog.Settings(
				settings.watchdogEnabled,
				settings.maxLinesInChange,
				settings.notificationIntervalInSeconds()
		));
		timerListener = new TimerEventsSource.Listener() {
			@Override public void onTimerUpdate(int seconds) {
				autoRevert.onTimer(seconds);
				changeSizeWatchdog.onTimer(seconds);
			}
		};

		onSettingsUpdate(settings);

		ApplicationManager.getApplication().getComponent(TimerEventsSource.class).addListener(timerListener);
	}

	@Override public void projectClosed() {
		super.projectClosed();
		ideNotifications.onProjectClosed();
		ApplicationManager.getApplication().getComponent(TimerEventsSource.class).removeListener(timerListener);
	}

	public void startAutoRevert() {
		autoRevert.start();
	}

	public boolean isAutoRevertStarted() {
		return autoRevert.isStarted();
	}

	public void stopAutoRevert() {
		autoRevert.stop();
	}

	@Override public void onSettingsUpdate(LimitedWIPSettings settings) {
		ideNotifications.onSettingsUpdate(settings);
		autoRevert.onSettings(new AutoRevert.Settings(
				settings.autoRevertEnabled,
				settings.secondsTillRevert(),
				settings.notifyOnRevert
		));
		changeSizeWatchdog.onSettings(new ChangeSizeWatchdog.Settings(
				settings.watchdogEnabled,
				settings.maxLinesInChange,
				settings.notificationIntervalInSeconds()
		));
	}

	public void onQuickCommit() {
		autoRevert.onAllFilesCommitted();
		changeSizeWatchdog.onCommit();
	}

    public void toggleSkipNotificationsUntilCommit() {
        boolean value = changeSizeWatchdog.toggleSkipNotificationsUntilCommit();
        ideNotifications.onSkipNotificationUntilCommit(value);
    }

	public void skipNotificationsUntilCommit(boolean value) {
		changeSizeWatchdog.skipNotificationsUntilCommit(value);
        ideNotifications.onSkipNotificationUntilCommit(value);
	}

    public void onVcsCommit(int uncommittedFilesSize) {
        if (uncommittedFilesSize == 0) {
            autoRevert.onAllFilesCommitted();
        }
        changeSizeWatchdog.onCommit();
    }
}
