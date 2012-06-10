package ru.groovy

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import ru.autorevert.Model
import ru.autorevert.TimerEventsSource
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.impl.CheckinHandlersManager

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

		// register commit callback
		def factories = CheckinHandlersManager.instance.getRegisteredCheckinHandlerFactories()
		factories.findAll {it.class.name == MyHandlerFactory.class.name}.each {
			CheckinHandlersManager.instance.unregisterCheckinHandlerFactory(it)
		}
		CheckinHandlersManager.instance.registerCheckinHandlerFactory(new MyHandlerFactory({
			model.onCommit()
		}))
	}

	@Override void disposeComponent() {
		super.disposeComponent()

		def timerEventsSource = ApplicationManager.application.getComponent(TimerEventsSource.class)
		timerEventsSource.removeListener(listener)

	}

	static class MyHandlerFactory extends CheckinHandlerFactory {
		Closure callback

		MyHandlerFactory(Closure callback) {
			this.callback = callback
		}

		@Override CheckinHandler createHandler(CheckinProjectPanel panel, CommitContext commitContext) {
			new CheckinHandler() {
				@Override void checkinSuccessful() {
					ChangeListManager.getInstance(panel.project).with {
						def uncommittedSize = defaultChangeList.changes.size() - panel.selectedChanges.size()
						if (uncommittedSize == 0) {
							callback.call();
						}
					}
				}
			}
		}
	}
}
