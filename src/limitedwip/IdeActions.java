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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.impl.ComparisonPolicy;
import com.intellij.openapi.diff.impl.fragments.LineFragment;
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.RollbackWorker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.diff.FilesTooBigForDiffException;
import com.intellij.util.ui.UIUtil;

import java.util.Collection;

import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*;
import static com.intellij.util.containers.ContainerUtil.map;
import static com.intellij.util.containers.ContainerUtil.toArray;

public class IdeActions {
	private static final Logger LOG = Logger.getInstance("#limitedwip.IdeActions");
	private final Project project;

	public IdeActions(Project project) {
		this.project = project;
	}

	public int currentChangeListSizeInLines() {
		final int[] result = {0};
		UIUtil.invokeAndWaitIfNeeded(new Runnable() {
			@Override public void run() {
				LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();
				TextCompareProcessor compareProcessor = new TextCompareProcessor(ComparisonPolicy.IGNORE_SPACE);

				for (Change change : changeList.getChanges()) {
					try {

						result[0] += amountOfChangedLinesIn(change, compareProcessor);

					} catch (VcsException ignored) {
					} catch (FilesTooBigForDiffException ignored) {
					}
				}
			}
		});
		return result[0];
	}

	private static int amountOfChangedLinesIn(Change change, TextCompareProcessor compareProcessor) throws VcsException, FilesTooBigForDiffException {
		ContentRevision beforeRevision = change.getBeforeRevision();
		ContentRevision afterRevision = change.getAfterRevision();

		ContentRevision revision = afterRevision;
		if (revision == null) revision = beforeRevision;
		if (revision == null || revision.getFile().getFileType().isBinary()) return 0;

		String contentBefore = beforeRevision != null ? beforeRevision.getContent() : "";
		String contentAfter = afterRevision != null ? afterRevision.getContent() : "";
		if (contentBefore == null) contentBefore = "";
		if (contentAfter == null) contentAfter = "";

		int result = 0;
		for (LineFragment fragment : compareProcessor.process(contentBefore, contentAfter)) {
			if (fragment.getType() == DELETED) {
				result += fragment.getModifiedLines1();
			} else if (fragment.getType() == CHANGED || fragment.getType() == CONFLICT || fragment.getType() == INSERT) {
				result += fragment.getModifiedLines2();
			}
		}
		return result;
	}

	public boolean revertCurrentChangeList() {
		final boolean[] result = new boolean[]{true};

		UIUtil.invokeAndWaitIfNeeded(new Runnable() {
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
					LOG.error("Error while doing revert", e);
				}
			}
		});

		return result[0];
	}
}
