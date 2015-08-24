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
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.RollbackWorker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import limitedwip.components.VcsIdeUtil;
import limitedwip.components.VcsIdeUtil.ChangeSize;

import java.util.Collection;

import static com.intellij.util.containers.ContainerUtil.map;
import static com.intellij.util.containers.ContainerUtil.toArray;

public class IdeActions {
	private static final Logger log = Logger.getInstance(IdeActions.class);
	private final Project project;
    private ChangeSize lastChangeSize = new ChangeSize(0);
    private int skipChecks;

    public IdeActions(Project project) {
		this.project = project;
	}

	public ChangeSize currentChangeListSizeInLines() {
        if (skipChecks > 0) {
            skipChecks--;
            return lastChangeSize;
        }
        ChangeSize changeSize = ApplicationManager.getApplication().runReadAction(new Computable<ChangeSize>() {
            @Override public ChangeSize compute() {
                LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();
                return VcsIdeUtil.currentChangeListSizeInLines(changeList.getChanges());
            }
        });
        if (changeSize.timedOut) {
            changeSize = new ChangeSize(lastChangeSize.value, true);
            skipChecks = 10;
        }
        lastChangeSize = changeSize;
        return changeSize;
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
