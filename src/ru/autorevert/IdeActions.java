package ru.autorevert;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.RollbackWorker;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
* User: dima
* Date: 12/06/2012
*/
public class IdeActions {
	private final Project project;

	public IdeActions(Project project) {
		this.project = project;
	}

	public void revertCurrentChangeList() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override public void run() {
					// TODO probably should detect commit dialog being open and don't revert in this case
					// TODO still get errors when commit overlaps auto-revert
					// TODO still get "reload from file system" popup questions
					LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();
					if (changeList.getChanges().isEmpty()) return;

					new RollbackWorker(project, true).doRollback(changeList.getChanges(), true, null, null);
					for (Change change : changeList.getChanges()) {
						FileDocumentManager.getInstance().reloadFiles(change.getVirtualFile());
					}

//					showPopup(project, "!" + changeList.getName() + " " + changeList.getChanges()); // TODO
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
