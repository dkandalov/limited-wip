package ru.autorevert;

import com.intellij.openapi.project.Project;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class Model {
	private boolean started = false;

	private final Project project;

	public Model(Project project) {
		this.project = project;
	}

	public synchronized void start() {
		started = true;
	}

	public synchronized void stop() {
		started = false;
	}
}
