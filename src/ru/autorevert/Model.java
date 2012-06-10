package ru.autorevert;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
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
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override public void run() {
					// TODO
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private void showPopup(final Project project, final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				ToolWindowManager.getInstance(project).notifyByBalloon(ToolWindowId.TODO_VIEW, MessageType.INFO, message);
			}
		});
	}
}
