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
import limitedwip.ui.settings.Settings;
import org.jetbrains.annotations.NotNull;
import limitedwip.IdeActions;
import limitedwip.IdeNotifications;

public class LimitedWIPProjectComponent extends AbstractProjectComponent implements Settings.Listener {
	private AutoRevert autoRevert;
	private TimerEventsSourceAppComponent.Listener listener;
	private IdeNotifications ideNotifications;

	protected LimitedWIPProjectComponent(Project project) {
		super(project);
	}

	@Override public void projectOpened() {
		super.projectOpened();

		Settings settings = ServiceManager.getService(Settings.class);
		ideNotifications = new IdeNotifications(myProject);
		autoRevert = new AutoRevert(ideNotifications, new IdeActions(myProject), settings.secondsTillRevert());

		onNewSettings(settings);

		TimerEventsSourceAppComponent timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSourceAppComponent.class);
		listener = new TimerEventsSourceAppComponent.Listener() {
			@Override public void onTimerEvent() {
				autoRevert.onTimer();
			}
		};
		timerEventsSource.addListener(listener);

		// register commit callback
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

		TimerEventsSourceAppComponent timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSourceAppComponent.class);
		timerEventsSource.removeListener(listener);
	}

	public void start() {
		autoRevert.start();
	}

	public boolean isStarted() {
		return autoRevert.isStarted();
	}

	public void stop() {
		autoRevert.stop();
	}

	@Override public void onNewSettings(Settings settings) {
		ideNotifications.onNewSettings(settings.showTimerInToolbar);
		autoRevert.onNewSettings(settings.secondsTillRevert());
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
