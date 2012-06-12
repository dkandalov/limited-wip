package ru.autorevert;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.RollbackWorker;
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

	public synchronized boolean isStarted() {
		return started;
	}

	public synchronized void onTimer() {
		if (!started) return;

		counter++;

		if (counter % TimeUnit.MINUTES.toSeconds(2) == 15) {
			showPopup(project, "15 seconds left to auto-revert");
		} else if (counter % TimeUnit.MINUTES.toSeconds(2) == 0) {
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
					// TODO probably should detect commit dialog being open and don't revert in this case
					// TODO still get "reload from file system" popup questions
					LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();
					if (changeList.getChanges().isEmpty()) return;

					new RollbackWorker(project, true).doRollback(changeList.getChanges(), true, null, null);
					for (Change change : changeList.getChanges()) {
						FileDocumentManager.getInstance().reloadFiles(change.getVirtualFile());
					}

					showPopup(project, "!" + changeList.getName() + " " + changeList.getChanges());
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
