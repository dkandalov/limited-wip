package ru.autorevert;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.checkin.BaseCheckinHandlerFactory;
import com.intellij.openapi.vcs.impl.CheckinHandlersManager;

import java.util.List;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class RevertComponent extends AbstractProjectComponent {
	private final Model model;
	private TimerEventsSource.Listener listener;

	protected RevertComponent(Project project) {
		super(project);
		model = new Model(project);
	}

	@Override public void initComponent() {
		super.initComponent();

		TimerEventsSource timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSource.class);
		listener = new TimerEventsSource.Listener() {
			@Override public void onTimerEvent() {
				model.onTimer();
			}
		};
		timerEventsSource.addListener(listener);

		// register commit callback
		List<BaseCheckinHandlerFactory> factories = CheckinHandlersManager.getInstance().getRegisteredCheckinHandlerFactories(new AbstractVcs[]{});

	}

	@Override public void disposeComponent() {
		super.disposeComponent();
	}
}
