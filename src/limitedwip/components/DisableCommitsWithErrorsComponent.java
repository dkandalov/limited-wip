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

import com.intellij.notification.*;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.problems.WolfTheProblemSolver;
import limitedwip.components.VcsUtil.CheckinListener;
import limitedwip.ui.settings.Settings;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.List;

import static limitedwip.components.VcsUtil.registerBeforeCheckInListener;

public class DisableCommitsWithErrorsComponent implements ApplicationComponent, Settings.Listener {
	private final Ref<Boolean> enabled = Ref.create(false);

	@Override public void initComponent() {
		registerBeforeCheckInListener(new CheckinListener() {
			@Override public boolean allowCheckIn(@NotNull Project project, @NotNull List<Change> changes) {
				if (!enabled.get()) return true;

				WolfTheProblemSolver wolf = WolfTheProblemSolver.getInstance(project);
				for (Module module : ModuleManager.getInstance(project).getModules()) {
					if (wolf.hasProblemFilesBeneath(module)) {
						notifyThatCommitWasCancelled();
						return false;
					}
				}
				return true;
			}
		});
	}

	private void notifyThatCommitWasCancelled() {
		NotificationListener listener = new NotificationListener() {
			@Override
			public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
				ShowSettingsUtil.getInstance().showSettingsDialog(null, LimitedWIPAppComponent.class);
			}
		};

		Notifications notificationsManager = (Notifications) NotificationsManager.getNotificationsManager();
		notificationsManager.notify(new Notification(
				LimitedWIPAppComponent.displayName,
				"You cannot commit because project has errors",
				"(It can be turned off in <a href=\"\">Settings</a> dialog)",
				NotificationType.WARNING,
				listener
		));
	}

	@Override public void disposeComponent() {
	}

	@NotNull @Override public String getComponentName() {
		return "DisableCommitsWithErrorsComponent";
	}

	@Override public void onSettings(Settings settings) {
		enabled.set(settings.disableCommitsWithErrors);
	}
}
