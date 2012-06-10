package ru.autorevert;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.impl.CheckinHandlersManager;
import org.jetbrains.annotations.NotNull;

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
		CheckinHandlersManager.getInstance().registerCheckinHandlerFactory(new MyHandlerFactory(new Runnable() {
			@Override public void run() {
				model.onCommit();
			}
		}));
	}

	@Override public void disposeComponent() {
		super.disposeComponent();

		TimerEventsSource timerEventsSource = ApplicationManager.getApplication().getComponent(TimerEventsSource.class);
		timerEventsSource.removeListener(listener);
	}

	public Model getModel() {
		return model;
	}

	private static class MyHandlerFactory extends CheckinHandlerFactory {
		private final Runnable callback;

		MyHandlerFactory(Runnable callback) {
			this.callback = callback;
		}

		@NotNull @Override
		public CheckinHandler createHandler(final CheckinProjectPanel panel, CommitContext commitContext) {
			return new CheckinHandler() {
				@Override public void checkinSuccessful() {
					ChangeListManager changeListManager = ChangeListManager.getInstance(panel.getProject());

					int uncommittedSize = changeListManager.getDefaultChangeList().getChanges().size() - panel.getSelectedChanges().size();
					if (uncommittedSize == 0) {
						callback.run();
					}
				}
			};
		}
	}
}
