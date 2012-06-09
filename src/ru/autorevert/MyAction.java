package ru.autorevert;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

/**
 * User: dima
 * Date: 09/06/2012
 */
public class MyAction extends AnAction {
	@Override public void actionPerformed(AnActionEvent actionEvent) {
		Project project = actionEvent.getProject();

	}
}
