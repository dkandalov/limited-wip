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

import com.intellij.ide.DataManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.actions.CommonCheckinProjectAction;
import com.intellij.openapi.vcs.changes.Change;
import limitedwip.common.PluginId;
import limitedwip.common.settings.LimitedWIPSettings;
import limitedwip.common.settings.LimitedWipConfigurable;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.List;

import static limitedwip.watchdog.components.VcsIdeUtil.registerBeforeCheckInListener;

public class DisableLargeCommitsAppComponent implements ApplicationComponent, LimitedWipConfigurable.Listener {
	private static final int maxShowCommitDialogAttempts = 3;

	private boolean enabled;
	private int maxChangeSizeInLines;
	private boolean allowCommitOnceWithoutCheck = false;

	@Override public void initComponent() {
		registerBeforeCheckInListener(new VcsIdeUtil.CheckinListener() {
			@Override public boolean allowCheckIn(@NotNull Project project, @NotNull List<Change> changes) {
				if (allowCommitOnceWithoutCheck) {
					allowCommitOnceWithoutCheck = false;
					return true;
				}
				if (!enabled) return true;

				WatchdogComponent watchdogComponent = project.getComponent(WatchdogComponent.class);
				if (watchdogComponent == null) return true;

				int changeSize = watchdogComponent.currentChangeListSize();
				if (changeSize > maxChangeSizeInLines) {
					notifyThatCommitWasCancelled(project);
					return false;
				}
				return true;
			}
		});
		LimitedWipConfigurable.registerSettingsListener(ApplicationManager.getApplication(), this);
	}

	private void notifyThatCommitWasCancelled(Project project) {
		NotificationListener listener = new NotificationListener() {
			@Override public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
				allowCommitOnceWithoutCheck = true;
				boolean succeeded = showCommitDialog(0);
				if (!succeeded) {
					allowCommitOnceWithoutCheck = false;
				}
			}
		};

		Notification notification = new Notification(
				PluginId.displayName,
				"Commit was cancelled because change size is above threshold<br/>",
				"(<a href=\"\">Click here</a> to force commit anyway)",
				NotificationType.ERROR,
				listener
		);
		project.getMessageBus().syncPublisher(Notifications.TOPIC).notify(notification);
	}

	/**
	 * Use retrying logic because of UI deadlock (not sure why exactly this happened):
	 *  "AWT-EventQueue-1 14.0.2#IU-139.658.4, eap:true" prio=0 tid=0x0 nid=0x0 waiting on condition
	 *  java.lang.Thread.State: WAITING on com.intellij.util.concurrency.Semaphore$Sync@4071d2c1
	 *  at sun.misc.Unsafe.park(Native Method)
	 *  at java.util.concurrent.locks.LockSupport.park(LockSupport.java:156)
	 *  at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:811)
	 *  at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedInterruptibly(AbstractQueuedSynchronizer.java:969)
	 *  at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireSharedInterruptibly(AbstractQueuedSynchronizer.java:1281)
	 *  at com.intellij.util.concurrency.Semaphore.waitForUnsafe(Semaphore.java:74)
	 *  at com.intellij.openapi.util.ActionCallback.waitFor(ActionCallback.java:269)
	 *  at com.intellij.openapi.util.AsyncResult.getResultSync(AsyncResult.java:147)
	 *  at com.intellij.openapi.util.AsyncResult.getResultSync(AsyncResult.java:142)
	 *  at limitedwip.watchdog.components.DisableLargeCommitsAppComponent$2.hyperlinkUpdate(DisableLargeCommitsAppComponent.java:65)
	 *  at com.intellij.notification.impl.ui.NotificationsUtil$1.hyperlinkUpdate(NotificationsUtil.java:75)
	 */
	private static boolean showCommitDialog(int showCommitDialogAttempts) {
		if (showCommitDialogAttempts > maxShowCommitDialogAttempts) {
			return false;
		}

		DataContext dataContext = DataManager.getInstance().getDataContextFromFocus().getResultSync(500);
		if (dataContext == null) {
			return showCommitDialog(showCommitDialogAttempts + 1);
		}

		AnActionEvent actionEvent = new AnActionEvent(
				null,
				dataContext,
				ActionPlaces.UNKNOWN,
				new Presentation(),
				ActionManager.getInstance(),
				0
		);
		new CommonCheckinProjectAction().actionPerformed(actionEvent);

		return true;
	}

	@Override public void disposeComponent() {
	}

	@NotNull @Override public String getComponentName() {
		return this.getClass().getCanonicalName();
	}

	@Override public void onSettingsUpdate(LimitedWIPSettings settings) {
		this.enabled = settings.disableCommitsAboveThreshold;
		this.maxChangeSizeInLines = settings.maxLinesInChange;
	}
}
