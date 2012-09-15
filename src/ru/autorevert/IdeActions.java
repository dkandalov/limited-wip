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
package ru.autorevert;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.RollbackWorker;
import com.intellij.util.ui.UIUtil;

/**
 * User: dima
 * Date: 12/06/2012
 */
public class IdeActions {
	private final Project project;

	public IdeActions(Project project) {
		this.project = project;
	}

	public boolean revertCurrentChangeList() {
		final boolean[] result = new boolean[]{true};

		UIUtil.invokeAndWaitIfNeeded(new Runnable() {
			@Override public void run() {
				LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();
				if (changeList.getChanges().isEmpty()) {
					result[0] = false;
					return;
				}

				new RollbackWorker(project, true).doRollback(changeList.getChanges(), true, null, null);
				for (Change change : changeList.getChanges()) {
					FileDocumentManager.getInstance().reloadFiles(change.getVirtualFile());
				}
			}
		});

		return result[0];
	}
}
