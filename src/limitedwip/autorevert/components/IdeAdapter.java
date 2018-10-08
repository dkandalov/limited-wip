package limitedwip.autorevert.components;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ui.RollbackWorker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Function;
import limitedwip.autorevert.AutoRevert;
import limitedwip.autorevert.ui.AutoRevertStatusBarWidget;
import limitedwip.common.PluginId;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intellij.util.containers.ContainerUtil.map;
import static com.intellij.util.containers.ContainerUtil.toArray;

public class IdeAdapter {
	private static final Logger logger = Logger.getInstance(IdeAdapter.class);

	private final AutoRevertStatusBarWidget autoRevertWidget = new AutoRevertStatusBarWidget();
	private final Project project;
	private AutoRevert.Settings settings;


	public IdeAdapter(final Project project) {
		this.project = project;
		Disposer.register(project, new Disposable() {
			@Override public void dispose() {
				StatusBar statusBar = statusBarFor(project);
				if (statusBar != null) {
					autoRevertWidget.showStoppedText();
					statusBar.removeWidget(autoRevertWidget.ID());
					statusBar.updateWidget(autoRevertWidget.ID());
				}
			}
		});
	}

	public int revertCurrentChangeList() {
		final AtomicInteger revertedFilesCount = new AtomicInteger(0);
		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			@Override public void run() {
				try {

					Collection<Change> changes = ChangeListManager.getInstance(project).getDefaultChangeList().getChanges();
					revertedFilesCount.set(changes.size());
					if (changes.isEmpty()) return;

					new RollbackWorker(project, "auto-revert", false).doRollback(changes, true, null, null);

					VirtualFile[] changedFiles = toArray(map(changes, new Function<Change, VirtualFile>() {
						@Override public VirtualFile fun(Change change) {
							return change.getVirtualFile();
						}
					}), new VirtualFile[0]);
					FileDocumentManager.getInstance().reloadFiles(changedFiles);

				} catch (Exception e) {
					// observed exception while reloading project at the time of auto-revert
					logger.error("Error while doing revert", e);
				}
			}
		});
		return revertedFilesCount.get();
	}

	public void onAutoRevertStarted(int timeEventsTillRevert) {
		if (settings.getShowTimerInToolbar()) {
			autoRevertWidget.showTime(formatTime(timeEventsTillRevert));
		} else {
			autoRevertWidget.showStartedText();
		}
		updateStatusBar();
	}

	public void onAutoRevertStopped() {
		autoRevertWidget.showStoppedText();
		updateStatusBar();
	}

	public void onChangesRevert() {
		Notification notification = new Notification(
                PluginId.displayName,
                PluginId.displayName,
				"Current changelist was reverted",
				NotificationType.WARNING
		);
		project.getMessageBus().syncPublisher(Notifications.TOPIC).notify(notification);
	}

	public void onCommit(int timeEventsTillRevert) {
		if (settings.getShowTimerInToolbar()) {
			autoRevertWidget.showTime(formatTime(timeEventsTillRevert));
		} else {
			autoRevertWidget.showStartedText();
		}
		updateStatusBar();
	}

	public void onTimeTillRevert(int secondsLeft) {
		if (settings.getShowTimerInToolbar()) {
			autoRevertWidget.showTime(formatTime(secondsLeft));
		} else {
			autoRevertWidget.showStartedText();
		}
		updateStatusBar();
	}

	public void onSettingsUpdate(AutoRevert.Settings settings) {
		this.settings = settings;
		updateStatusBar();
	}

	private void updateStatusBar() {
		StatusBar statusBar = statusBarFor(project);
		if (statusBar == null) return;

		boolean hasAutoRevertWidget = statusBar.getWidget(autoRevertWidget.ID()) != null;
		if (hasAutoRevertWidget && settings.getAutoRevertEnabled()) {
			statusBar.updateWidget(autoRevertWidget.ID());

		} else if (hasAutoRevertWidget) {
			statusBar.removeWidget(autoRevertWidget.ID());

		} else if (settings.getAutoRevertEnabled()) {
			autoRevertWidget.showStoppedText();
			statusBar.addWidget(autoRevertWidget, "before Position");
			statusBar.updateWidget(autoRevertWidget.ID());
		}
	}

	private static StatusBar statusBarFor(Project project) {
		return WindowManager.getInstance().getStatusBar(project);
	}

	private static String formatTime(int seconds) {
		int min = seconds / 60;
		int sec = seconds % 60;
		return String.format("%02d", min) + ":" + String.format("%02d", sec);
	}
}

