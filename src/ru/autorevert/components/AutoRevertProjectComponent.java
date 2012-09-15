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
package ru.autorevert.components;

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
import org.jetbrains.annotations.NotNull;
import ru.autorevert.IdeActions;
import ru.autorevert.IdeNotifications;
import ru.autorevert.Model;
import ru.autorevert.settings.Settings;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class AutoRevertProjectComponent extends AbstractProjectComponent implements Settings.Listener {
	Model model;
	private TimerEventsSourceAppComponent.Listener listener;
	private IdeNotifications ideNotifications;

	protected AutoRevertProjectComponent(Project project) {
		super(project);
	}

	@Override public void projectOpened() {
		super.projectOpened();

		Settings settings = ServiceManager.getService(Settings.class);
		ideNotifications = new IdeNotifications(myProject);
		model = new Model(ideNotifications, new IdeActions(myProject), settings.secondsTillRevert());

		onNewSettings(settings);

		TimerEventsSourceAppComponent timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSourceAppComponent.class);
		listener = new TimerEventsSourceAppComponent.Listener() {
			@Override public void onTimerEvent() {
				model.onTimer();
			}
		};
		timerEventsSource.addListener(listener);

		// register commit callback
		CheckinHandlersManager.getInstance().registerCheckinHandlerFactory(new MyHandlerFactory(myProject, new Runnable() {
			@Override public void run() {
				model.onCommit();
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
		model.start();
	}

	public boolean isStarted() {
		return model.isStarted();
	}

	public void stop() {
		model.stop();
	}

	@Override public void onNewSettings(Settings settings) {
		ideNotifications.onNewSettings(settings.showTimerInToolbar);
		model.onNewSettings(settings.secondsTillRevert());
	}

	private static class MyHandlerFactory extends CheckinHandlerFactory {
		private final Project project;
		private final Runnable callback;

		MyHandlerFactory(Project project, Runnable callback) {
			this.project = project;
			this.callback = callback;
		}

		@NotNull @Override
		public CheckinHandler createHandler(final CheckinProjectPanel panel, CommitContext commitContext) {
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
