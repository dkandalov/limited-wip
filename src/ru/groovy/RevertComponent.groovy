package ru.groovy

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager
import ru.autorevert.TimerEventsSource

/**
 * User: dima
 * Date: 09/06/2012
 */
public class RevertComponent extends AbstractProjectComponent {
	Model model
	TimerEventsSource.Listener listener

	RevertComponent(Project project) {
		super(project)

		model = new Model(project)
	}

	@Override void initComponent() {
		super.initComponent()

		def timerEventsSource = ApplicationManager.application.getComponent(TimerEventsSource.class)
		listener = new TimerEventsSource.Listener() {
			@Override void onTimerEvent() {
				model.onTimer()
			}
		}
		timerEventsSource.addListener(listener)

		// TODO register commit callback
	}

	@Override void disposeComponent() {
		super.disposeComponent()

		def timerEventsSource = ApplicationManager.application.getComponent(TimerEventsSource.class)
		timerEventsSource.removeListener(listener)

	}
}
