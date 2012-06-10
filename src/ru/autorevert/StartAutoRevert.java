package ru.autorevert;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class StartAutoRevert extends AnAction {
	@Override public void actionPerformed(AnActionEvent event) {
		Project project = event.getProject();
		if (project == null) return;

		RevertComponent revertComponent = project.getComponent(RevertComponent.class);
		revertComponent.getModel().start();
	}
}
