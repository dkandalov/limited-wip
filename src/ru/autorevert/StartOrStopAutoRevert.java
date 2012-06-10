package ru.autorevert;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class StartOrStopAutoRevert extends AnAction {
	@Override public void update(AnActionEvent event) {
		Project project = event.getProject();
		if (project == null) return;

		Model model = project.getComponent(RevertComponent.class).getModel();
		String text;
		if (model.isStarted()) {
			text = "Stop auto-revert";
		} else {
			text = "Start auto-revert";
		}
		event.getPresentation().setText(text);
	}

	@Override public void actionPerformed(AnActionEvent event) {
		Project project = event.getProject();
		if (project == null) return;

		Model model = project.getComponent(RevertComponent.class).getModel();
		if (model.isStarted()) {
			model.stop();
		} else {
			model.start();
		}
	}
}
