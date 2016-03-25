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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.diff.FilesTooBigForDiffException;
import limitedwip.watchdog.ChangeSize;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*;

public class ChangeSizeCalculator {
	private static final long diffDurationThresholdMillis = 200;
	private final ChangeSizeCache changeSizeCache;
	private final Project project;
	private ChangeSize changeSize;


	public ChangeSizeCalculator(Project project) {
		this.project = project;
		this.changeSizeCache = new ChangeSizeCache();
	}

	public void onTimer() {
		changeSize = ApplicationManager.getApplication().runReadAction(new Computable<ChangeSize>() {
			@Override public ChangeSize compute() {
				return calculateCurrentChangeListSizeInLines();
			}
		});
	}

	public ChangeSize currentChangeListSizeInLines() {
		return changeSize;
	}

	/**
	 * Can't use com.intellij.openapi.vcs.impl.LineStatusTrackerManager here because it only tracks changes for open files.
	 */
	private ChangeSize calculateCurrentChangeListSizeInLines() {
		long startTime = System.currentTimeMillis();
		TextCompareProcessor compareProcessor = new TextCompareProcessor(
				ComparisonPolicy.TRIM_SPACE,
				DiffPolicy.LINES_WO_FORMATTING,
				HighlightMode.BY_LINE
		);
		LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();

		ChangeSize result = new ChangeSize(0);
		for (Change change : changeList.getChanges()) {
			Document document = getDocumentFor(change);

			long duration = System.currentTimeMillis() - startTime;
			if (duration > diffDurationThresholdMillis) {
				result = new ChangeSize(result.value, true);
				break;
			}

			ChangeSize changeSize = changeSizeCache.get(document);
			if (changeSize == null) {
				changeSize = currentChangeListSizeInLines(change, startTime, compareProcessor);
				if (!changeSize.isApproximate) {
					changeSizeCache.put(document, changeSize);
				}
			}
			result = result.add(changeSize);
		}
		return result;
	}

	@Nullable private static Document getDocumentFor(Change change) {
		VirtualFile virtualFile = change.getVirtualFile();
		return virtualFile == null ? null : FileDocumentManager.getInstance().getDocument(virtualFile);
	}

	private static ChangeSize currentChangeListSizeInLines(Change change, long startTime, TextCompareProcessor compareProcessor) {
		try {

			return amountOfChangedLinesIn(change, compareProcessor, startTime);

		} catch (VcsException ignored) {
			return new ChangeSize(0, true);
		} catch (FilesTooBigForDiffException ignored) {
			return new ChangeSize(0, true);
		}
	}

	private static ChangeSize amountOfChangedLinesIn(Change change, TextCompareProcessor compareProcessor, long startTime) throws VcsException, FilesTooBigForDiffException {
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
			long duration = System.currentTimeMillis() - startTime;
			if (duration > diffDurationThresholdMillis) {
				return new ChangeSize(result, true);
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
