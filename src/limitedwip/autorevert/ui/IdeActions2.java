package limitedwip.autorevert.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ui.RollbackWorker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;

import java.util.Collection;

import static com.intellij.util.containers.ContainerUtil.map;
import static com.intellij.util.containers.ContainerUtil.toArray;

public class IdeActions2 {
	private static final Logger log = Logger.getInstance(IdeActions2.class);
	private final Project project;

	public IdeActions2(Project project) {
		this.project = project;
	}

	public void revertCurrentChangeList() {
		final Application application = ApplicationManager.getApplication();
		application.invokeLater(new Runnable() {
			@Override public void run() {
				application.runWriteAction(new Runnable() {
					@Override public void run() {
						try {

							Collection<Change> changes = ChangeListManager.getInstance(project).getDefaultChangeList().getChanges();
							if (changes.isEmpty()) return;

							new RollbackWorker(project, "auto-revert", false).doRollback(changes, true, null, null);

							VirtualFile[] changedFiles = toArray(map(changes, new Function<Change, VirtualFile>() {
								@Override public VirtualFile fun(Change change) {
									return change.getVirtualFile();
								}
							}), new VirtualFile[changes.size()]);
							FileDocumentManager.getInstance().reloadFiles(changedFiles);

						} catch (Exception e) {
							// observed exception while reloading project at the time of auto-revert
							log.error("Error while doing revert", e);
						}
					}
				});
			}
		});
	}

}
