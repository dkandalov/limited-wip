package ru.autorevert;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class StartOrStopAutoRevertAction extends AnAction {
	@Override public void actionPerformed(AnActionEvent event) {
		Project project = event.getProject();
		if (project == null) return;

		RevertComponent revertComponent = project.getComponent(RevertComponent.class);
		if (revertComponent.isStarted()) {
			revertComponent.stop();
		} else {
			revertComponent.start();
		}
	}

	@Override public void update(AnActionEvent event) {
		String text = textFor(event.getProject());
		event.getPresentation().setText(text);
		event.getPresentation().setDescription(text);
		event.getPresentation().setEnabled(event.getProject() != null);
	}

	private static String textFor(Project project) {
		if (project == null) return "Start auto-revert";

		if (project.getComponent(RevertComponent.class).isStarted()) {
			return "Stop auto-revert";
		} else {
			return "Start auto-revert";
		}
	}
}
