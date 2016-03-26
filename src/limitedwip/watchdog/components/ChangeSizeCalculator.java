package limitedwip.watchdog.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diff.impl.ComparisonPolicy;
import com.intellij.openapi.diff.impl.fragments.LineFragment;
import com.intellij.openapi.diff.impl.processing.DiffPolicy;
import com.intellij.openapi.diff.impl.processing.HighlightMode;
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.diff.FilesTooBigForDiffException;
import limitedwip.common.PluginId;
import limitedwip.watchdog.ChangeSize;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*;

public class ChangeSizeCalculator {
	private final ChangeSizeCache changeSizeCache;
	private final Project project;
	private ChangeSize changeSize;
	private volatile boolean isRunningBackgroundDiff;


	public ChangeSizeCalculator(Project project) {
		this.project = project;
		this.changeSize = new ChangeSize(0, true);
		this.changeSizeCache = new ChangeSizeCache();
	}

	public ChangeSize currentChangeListSizeInLines() {
		return changeSize;
	}

	public void onTimer() {
		calculateCurrentChangeListSizeInLines();
	}

	/**
	 * Can't use com.intellij.openapi.vcs.impl.LineStatusTrackerManager here because it only tracks changes for open files.
	 */
	private void calculateCurrentChangeListSizeInLines() {
		if (isRunningBackgroundDiff) return;

		final Pair<ChangeSize, List<Change>> pair = ApplicationManager.getApplication().runReadAction(new Computable<Pair<ChangeSize, List<Change>>>() {
			@Override public Pair<ChangeSize, List<Change>> compute() {
				LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();
				List<Change> changesToDiff = new ArrayList<Change>();

				ChangeSize result = new ChangeSize(0);
				for (Change change : changeList.getChanges()) {
					Document document = getDocumentFor(change);
					ChangeSize changeSize = changeSizeCache.get(document);
					if (changeSize == null) {
						changesToDiff.add(change);
					} else {
						result = result.add(changeSize);
					}
				}
				return Pair.create(result, changesToDiff);
			}
		});
		if (pair.second.isEmpty()) {
			changeSize = pair.first;
			return;
		}

		new Thread(new Runnable() {
			@Override public void run() {
				isRunningBackgroundDiff = true;

				TextCompareProcessor compareProcessor = new TextCompareProcessor(
						ComparisonPolicy.TRIM_SPACE,
						DiffPolicy.LINES_WO_FORMATTING,
						HighlightMode.BY_LINE
				);
				final Map<Change, ChangeSize> changeSizeByChange = new HashMap<Change, ChangeSize>();
				for (Change change : pair.second) {
					changeSizeByChange.put(change, currentChangeListSizeInLines(change, compareProcessor));
				}

				ApplicationManager.getApplication().invokeLater(new Runnable() {
					@Override public void run() {
						changeSize = pair.first;
						for (ChangeSize it : changeSizeByChange.values()) {
							changeSize = changeSize.add(it);
						}
						for (Map.Entry<Change, ChangeSize> entry : changeSizeByChange.entrySet()) {
							Document document = getDocumentFor(entry.getKey());
							if (document != null && !entry.getValue().isApproximate) {
								changeSizeCache.put(document, entry.getValue());
							}
						}
						isRunningBackgroundDiff = false;
					}
				});
			}
		}, PluginId.value + "-DiffThread").start();
	}

	@Nullable private static Document getDocumentFor(Change change) {
		VirtualFile virtualFile = change.getVirtualFile();
		return virtualFile == null ? null : FileDocumentManager.getInstance().getDocument(virtualFile);
	}

	private static ChangeSize currentChangeListSizeInLines(Change change, TextCompareProcessor compareProcessor) {
		try {

			return amountOfChangedLinesIn(change, compareProcessor);

		} catch (VcsException ignored) {
			return new ChangeSize(0, true);
		} catch (FilesTooBigForDiffException ignored) {
			return new ChangeSize(0, true);
		}
	}

	private static ChangeSize amountOfChangedLinesIn(Change change, TextCompareProcessor compareProcessor) throws VcsException, FilesTooBigForDiffException {
		ContentRevision beforeRevision = change.getBeforeRevision();
		ContentRevision afterRevision = change.getAfterRevision();
		if (beforeRevision instanceof FakeRevision || afterRevision instanceof FakeRevision) {
			return new ChangeSize(0, true);
		}

		ContentRevision revision = afterRevision;
		if (revision == null) revision = beforeRevision;
		if (revision == null || revision.getFile().getFileType().isBinary()) return new ChangeSize(0);

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
		return new ChangeSize(result);
	}


	private static class ChangeSizeCache {
		private final Map<Document, ChangeSize> changeSizeByDocument = new HashMap<Document, ChangeSize>();

		public void put(final Document document, ChangeSize changeSize) {
			if (document == null) return;
			changeSizeByDocument.put(document, changeSize);
			document.addDocumentListener(new DocumentListener() {
				@Override public void beforeDocumentChange(DocumentEvent event) {}
				@Override public void documentChanged(DocumentEvent event) {
					remove(document);
					document.removeDocumentListener(this);
				}
			});
		}

		public void remove(Document document) {
			changeSizeByDocument.remove(document);
		}

		public ChangeSize get(Document document) {
			if (document == null) return null;
			return changeSizeByDocument.get(document);
		}
	}
}
