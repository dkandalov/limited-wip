package limitedwip.autorevert.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import limitedwip.autorevert.AutoRevert;
import limitedwip.common.LimitedWIPSettings;
import limitedwip.common.TimerEventsSource;

public class AutoRevertComponent extends AbstractProjectComponent implements LimitedWIPSettings.Listener  {
	private final TimerEventsSource timerEventsSource;
	private AutoRevert autoRevert;
	private IdeNotifications2 ideNotifications;

	protected AutoRevertComponent(Project project) {
		super(project);
		timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSource.class);
	}

	@Override public void projectOpened() {
		LimitedWIPSettings settings = ServiceManager.getService(LimitedWIPSettings.class);
		ideNotifications = new IdeNotifications2(myProject, settings);
		IdeActions2 ideActions = new IdeActions2(myProject);
		autoRevert = new AutoRevert(ideNotifications, ideActions, new AutoRevert.Settings(
				settings.autoRevertEnabled,
				settings.secondsTillRevert(),
				settings.notifyOnRevert
		));
		TimerEventsSource.Listener timerListener = new TimerEventsSource.Listener() {
			@Override public void onTimerUpdate(int seconds) {
				autoRevert.onTimer(seconds);
			}
		};

		onSettingsUpdate(settings);
		timerEventsSource.addListener(timerListener, myProject);
	}

	@Override public void onSettingsUpdate(LimitedWIPSettings settings) {
		ideNotifications.onSettingsUpdate(settings);
		autoRevert.onSettings(new AutoRevert.Settings(
				settings.autoRevertEnabled,
				settings.secondsTillRevert(),
				settings.notifyOnRevert
		));
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

	public void onVcsCommit(int uncommittedFilesSize) {
		if (uncommittedFilesSize == 0) {
			autoRevert.onAllFilesCommitted();
		}
	}

	@Override public void projectClosed() {
		super.projectClosed();
		ideNotifications.onProjectClosed();
	}
}
