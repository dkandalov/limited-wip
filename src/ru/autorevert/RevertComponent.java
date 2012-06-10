package ru.autorevert;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class RevertComponent extends AbstractProjectComponent {
	private final Model model;
	private final TimerEventsSource.Listener listener;

	protected RevertComponent(Project project) {
		super(project);
		model = new Model(project);
		listener = new TimerEventsSource.Listener() {
			@Override public void onTimerEvent() {

			}
		};
	}

	@Override public void initComponent() {
		super.initComponent();

		TimerEventsSource timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSource.class);
		timerEventsSource.addListener(listener);

	}

	@Override public void disposeComponent() {
		super.disposeComponent();
	}
}
