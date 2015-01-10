/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package limitedwip;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.RollbackWorker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.ui.UIUtil;
import limitedwip.components.VcsIdeUtil;

import java.util.Collection;

import static com.intellij.util.containers.ContainerUtil.map;
import static com.intellij.util.containers.ContainerUtil.toArray;

public class IdeActions {
	private static final Logger log = Logger.getInstance(IdeActions.class);
	private final Project project;

	public IdeActions(Project project) {
		this.project = project;
	}

	public int currentChangeListSizeInLines() {
		final int[] result = {0};
		UIUtil.invokeAndWaitIfNeeded(new Runnable() {
			@Override public void run() {
				LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();
				result[0] = VcsIdeUtil.currentChangeListSizeInLines(changeList.getChanges());
			}
		});
		return result[0];
	}
	
	public boolean revertCurrentChangeList() {
		final boolean[] result = new boolean[]{true};

		UIUtil.invokeAndWaitIfNeeded(new Runnable() {
			@Override public void run() {
				Application application = ApplicationManager.getApplication();
				application.runWriteAction(new Runnable() {
					@Override public void run() {
						try {

							Collection<Change> changes = ChangeListManager.getInstance(project).getDefaultChangeList().getChanges();
							if (changes.isEmpty()) {
								result[0] = false;
								return;
							}

							new RollbackWorker(project, "auto-revert").doRollback(changes, true, null, null);

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

		return result[0];
	}
}
