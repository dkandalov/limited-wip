package ru.autorevert;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class RevertComponent extends AbstractProjectComponent {

	protected RevertComponent(Project project) {
		super(project);
	}
}
