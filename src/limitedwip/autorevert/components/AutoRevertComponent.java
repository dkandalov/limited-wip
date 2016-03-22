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

	protected AutoRevertComponent(Project project) {
		super(project);
		timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSource.class);
	}

	@Override public void projectOpened() {
		LimitedWIPSettings settings = ServiceManager.getService(LimitedWIPSettings.class);
		IdeNotifications2 ideNotifications = new IdeNotifications2(myProject);
		autoRevert = new AutoRevert(ideNotifications, new IdeActions2(myProject)).init(convert(settings));

		timerEventsSource.addListener(new TimerEventsSource.Listener() {
			@Override public void onTimerUpdate(int seconds) {
				autoRevert.onTimer(seconds);
			}
		}, myProject);
	}

	@Override public void onSettingsUpdate(LimitedWIPSettings settings) {
		autoRevert.onSettings(convert(settings));
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

	private static AutoRevert.Settings convert(LimitedWIPSettings settings) {
		return new AutoRevert.Settings(
				settings.autoRevertEnabled,
				settings.secondsTillRevert(),
				settings.notifyOnRevert
		);
	}
}
