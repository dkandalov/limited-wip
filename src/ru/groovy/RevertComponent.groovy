package ru.groovy

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project

/**
 * User: dima
 * Date: 09/06/2012
 */
public class RevertComponent extends AbstractProjectComponent {
	RevertComponent(Project project) {
		super(project)
	}

	def onTimer() {

	}
}
