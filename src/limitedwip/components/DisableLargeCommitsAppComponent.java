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

import com.intellij.ide.DataManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.actions.CommonCheckinProjectAction;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import limitedwip.components.VcsIdeUtil.CheckinListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.List;

import static limitedwip.components.VcsIdeUtil.registerBeforeCheckInListener;

public class DisableLargeCommitsAppComponent implements ApplicationComponent {
	private boolean enabled;
	private int maxLinesInChange;
	private boolean allowOnce;

	@Override public void initComponent() {
		registerBeforeCheckInListener(new CheckinListener() {
			@Override public boolean allowCheckIn(@NotNull Project project, @NotNull List<Change> changes) {
				if (allowOnce) {
					allowOnce = false;
					return true;
				}
				if (!enabled) return true;

				LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();
				int changeListSize = VcsIdeUtil.currentChangeListSizeInLines(changeList.getChanges());
				if (changeListSize > maxLinesInChange) {
					notifyThatCommitWasCancelled(project);
					return false;
				}
				return true;
			}
		});
	}

	private void notifyThatCommitWasCancelled(Project project) {
		NotificationListener listener = new NotificationListener() {
			@Override public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
				allowOnce = true;
				AnActionEvent actionEvent = new AnActionEvent(
						null,
						DataManager.getInstance().getDataContextFromFocus().getResultSync(),
						ActionPlaces.UNKNOWN,
						new Presentation(),
						ActionManager.getInstance(),
						0
				);
				new CommonCheckinProjectAction().actionPerformed(actionEvent);
			}
		};

		Notification notification = new Notification(
				LimitedWIPAppComponent.displayName,
				"Commit was cancelled because change size is above threshold<br/>",
				"(<a href=\"\">Click here</a> to force commit anyway)",
				NotificationType.ERROR,
				listener
		);
		project.getMessageBus().syncPublisher(Notifications.TOPIC).notify(notification);
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
