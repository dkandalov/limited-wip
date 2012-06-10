package ru.autorevert;

import com.intellij.openapi.project.Project;

import java.util.concurrent.TimeUnit;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class Model {
	private boolean started = false;
	private final Project project;
	private int counter;

	public Model(Project project) {
		this.project = project;
	}

	public synchronized void start() {
		started = true;
		counter = 0;
	}

	public synchronized void stop() {
		started = false;
	}

	public synchronized void onTimer() {
		if (!started) return;

		counter++;
		showPopup(project, "" + counter);

		if (counter % TimeUnit.MINUTES.toSeconds(2) == 0) {
			revertChanges();
			counter = 0;
		}
	}

	public synchronized void onCommit() {
		counter = 0;
	}

	private void revertChanges() {
		// TODO implement

	}

	private void showPopup(Project project, String message) {
		// TODO implement

	}
}
