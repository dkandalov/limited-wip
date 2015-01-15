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
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.impl.CheckinHandlersManager;
import com.intellij.util.Consumer;
import limitedwip.AutoRevert;
import limitedwip.ChangeSizeWatchdog;
import limitedwip.IdeActions;
import limitedwip.IdeNotifications;
import limitedwip.ui.settings.Settings;
import org.jetbrains.annotations.NotNull;

public class LimitedWIPProjectComponent extends AbstractProjectComponent implements Settings.Listener {
	private ChangeSizeWatchdog changeSizeWatchdog;
	private AutoRevert autoRevert;
	private IdeNotifications ideNotifications;

	private TimerEventsSource.Listener timerListener;
	private CheckinSizeHandlerFactory checkinSizeHandlerFactory;


	public LimitedWIPProjectComponent(Project project) {
		super(project);
	}

	@Override public void projectOpened() {
		super.projectOpened();

		Settings settings = ServiceManager.getService(Settings.class);
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
		checkinSizeHandlerFactory = new CheckinSizeHandlerFactory(myProject, new Consumer<Integer>() {
			@Override public void consume(Integer uncommittedFilesSize) {
				if (uncommittedFilesSize == 0) {
					autoRevert.onAllFilesCommitted();
				}
				changeSizeWatchdog.onCommit();
			}
		});

		onSettings(settings);

		ApplicationManager.getApplication().getComponent(TimerEventsSource.class).addListener(timerListener);
		CheckinHandlersManager.getInstance().registerCheckinHandlerFactory(checkinSizeHandlerFactory);
	}

	@Override public void projectClosed() {
		super.projectClosed();
		ideNotifications.onProjectClosed();
		ApplicationManager.getApplication().getComponent(TimerEventsSource.class).removeListener(timerListener);
		CheckinHandlersManager.getInstance().unregisterCheckinHandlerFactory(checkinSizeHandlerFactory);
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

	@Override public void onSettings(Settings settings) {
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

	public void skipNotificationsUntilCommit(boolean value) {
		changeSizeWatchdog.skipNotificationsUntilCommit(value);
	}


	private static class CheckinSizeHandlerFactory extends CheckinHandlerFactory {
		private final Project project;
		private final Consumer<Integer> callback;

		public CheckinSizeHandlerFactory(Project project, Consumer<Integer> callback) {
			this.project = project;
			this.callback = callback;
		}

		@NotNull @Override
		public CheckinHandler createHandler(@NotNull final CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
			return new CheckinHandler() {
				@Override public void checkinSuccessful() {
					if (!project.equals(panel.getProject())) return;

					ChangeListManager changeListManager = ChangeListManager.getInstance(panel.getProject());
					int uncommittedFilesSize = changeListManager.getDefaultChangeList().getChanges().size() - panel.getSelectedChanges().size();
					callback.consume(uncommittedFilesSize);
				}
			};
		}
	}
}
