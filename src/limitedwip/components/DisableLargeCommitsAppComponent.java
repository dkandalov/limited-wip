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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import limitedwip.components.VcsIdeUtil.CheckinListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.List;

import static limitedwip.components.VcsIdeUtil.registerBeforeCheckInListener;

public class DisableLargeCommitsAppComponent implements ApplicationComponent {
	private boolean enabled;
	private int maxLinesInChange;

	@Override public void initComponent() {
		registerBeforeCheckInListener(new CheckinListener() {
			@Override public boolean allowCheckIn(@NotNull Project project, @NotNull List<Change> changes) {
				if (!enabled) return true;

				int changeListSize = VcsIdeUtil.currentChangeListSizeInLines(changes);
				if (changeListSize > maxLinesInChange) {
					notifyThatCommitWasCancelled();
					return false;
				}
				return true;
			}
		});
	}

	private void notifyThatCommitWasCancelled() {
		NotificationListener listener = new NotificationListener() {
			@Override public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
				ShowSettingsUtil.getInstance().showSettingsDialog(null, LimitedWIPAppComponent.class);
			}
		};

		Notification notification = new Notification(
				LimitedWIPAppComponent.displayName,
				"Commit was cancelled because change size is above threshold<br/>",
				"(This can be reconfigured in IDE <a href=\"\">Settings</a>)",
				NotificationType.WARNING,
				listener
		);
		ApplicationManager.getApplication().getMessageBus().syncPublisher(Notifications.TOPIC).notify(notification);
	}

	@Override public void disposeComponent() {
	}

	@NotNull @Override public String getComponentName() {
		return this.getClass().getCanonicalName();
	}

	public void onSettingsUpdate(boolean enabled, int maxLinesInChange) {
		this.enabled = enabled;
		this.maxLinesInChange = maxLinesInChange;
	}
}
