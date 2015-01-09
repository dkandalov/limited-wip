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
import limitedwip.AutoRevert;
import limitedwip.ChangeSizeWatchdog;
import limitedwip.IdeActions;
import limitedwip.IdeNotifications;
import limitedwip.ui.settings.Settings;
import org.jetbrains.annotations.NotNull;

public class LimitedWIPProjectComponent extends AbstractProjectComponent implements Settings.Listener {
	private AutoRevert autoRevert;
	private TimerEventsSource.Listener listener;
	private IdeNotifications ideNotifications;
	private ChangeSizeWatchdog changeSizeWatchdog;
	private IdeActions ideActions;

	protected LimitedWIPProjectComponent(Project project) {
		super(project);
	}

	@Override public void projectOpened() {
		super.projectOpened();

		Settings settings = ServiceManager.getService(Settings.class);
		ideNotifications = new IdeNotifications(myProject);
		ideActions = new IdeActions(myProject);
		autoRevert = new AutoRevert(ideNotifications, ideActions, new AutoRevert.Settings(
				settings.autoRevertEnabled,
				settings.secondsTillRevert()
		));
		changeSizeWatchdog = new ChangeSizeWatchdog(ideNotifications, new ChangeSizeWatchdog.Settings(
				settings.disableCommitsAboveThreshold,
				settings.maxLinesInChange
		));

		onNewSettings(settings);

		TimerEventsSource timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSource.class);
		listener = new TimerEventsSource.Listener() {
			@Override public void onTimerUpdate(int seconds) {
				autoRevert.onTimer(seconds);
				changeSizeWatchdog.onChangeSizeUpdate(ideActions.currentChangeListSizeInLines(), seconds);
			}
		};
		timerEventsSource.addListener(listener);

		CheckinHandlersManager.getInstance().registerCheckinHandlerFactory(new MyHandlerFactory(myProject, new Runnable() {
			@Override public void run() {
				autoRevert.onCommit();
			}
		}));
	}

	@Override public void projectClosed() {
		super.projectClosed();
		ideNotifications.onProjectClosed();
	}

	@Override public void disposeComponent() {
		super.disposeComponent();

		TimerEventsSource timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSource.class);
		timerEventsSource.removeListener(listener);
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

	@Override public void onNewSettings(Settings settings) {
		ideNotifications.onNewSettings(
				settings.showTimerInToolbar,
				settings.autoRevertEnabled
		);
		autoRevert.on(new AutoRevert.Settings(
				settings.autoRevertEnabled,
				settings.secondsTillRevert())
		);
		changeSizeWatchdog.onNewSettings(
				settings.maxLinesInChange,
				settings.disableCommitsAboveThreshold
		);
	}

	public void onQuickCommit() {
		autoRevert.onCommit();
	}

	private static class MyHandlerFactory extends CheckinHandlerFactory {
		private final Project project;
		private final Runnable callback;

		MyHandlerFactory(Project project, Runnable callback) {
			this.project = project;
			this.callback = callback;
		}

		@NotNull @Override
		public CheckinHandler createHandler(@NotNull final CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
			return new CheckinHandler() {
				@Override public void checkinSuccessful() {
					if (!project.equals(panel.getProject())) return;

					ChangeListManager changeListManager = ChangeListManager.getInstance(panel.getProject());
					int uncommittedSize = changeListManager.getDefaultChangeList().getChanges().size() - panel.getSelectedChanges().size();
					if (uncommittedSize == 0) {
						callback.run();
					}
				}
			};
		}
	}
}
