package limitedwip.autorevert.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import limitedwip.autorevert.AutoRevert;
import limitedwip.common.LimitedWipCheckin;
import limitedwip.common.TimerComponent;
import limitedwip.common.settings.LimitedWIPSettings;
import limitedwip.common.settings.LimitedWipConfigurable;

public class AutoRevertComponent extends AbstractProjectComponent implements LimitedWipConfigurable.Listener, LimitedWipCheckin.Listener {
	private final TimerComponent timer;
	private AutoRevert autoRevert;

	protected AutoRevertComponent(Project project) {
		super(project);
		timer = ApplicationManager.getApplication().getComponent(TimerComponent.class);
	}

	@Override public void projectOpened() {
		LimitedWIPSettings settings = ServiceManager.getService(LimitedWIPSettings.class);
		autoRevert = new AutoRevert(new IdeAdapter(myProject)).init(convert(settings));

		timer.addListener(new TimerComponent.Listener() {
			@Override public void onUpdate(final int seconds) {
				ApplicationManager.getApplication().invokeLater(new Runnable() {
					@Override public void run() {
						autoRevert.onTimer(seconds);
					}
				}, ModalityState.any());
			}
		}, myProject);

		LimitedWipConfigurable.registerSettingsListener(myProject, this);
		LimitedWipCheckin.registerListener(myProject, this);
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

	public void onSuccessfulCheckin(boolean allFileAreCommitted) {
		if (allFileAreCommitted) {
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
