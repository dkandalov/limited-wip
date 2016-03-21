package limitedwip.watchdog.components;

import com.intellij.openapi.components.AbstractProjectComponent;
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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.diff.FilesTooBigForDiffException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*;

public class ChangeSizeProjectComponent extends AbstractProjectComponent {
	private static final long diffDurationThresholdMillis = 200;

	private final Set<Document> unchanged = new HashSet<Document>();
	private final Map<Document, ChangeSize> changeSizeByDocument = new HashMap<Document, ChangeSize>();


	public static ChangeSizeProjectComponent getInstance(Project project) {
		return project.getComponent(ChangeSizeProjectComponent.class);
	}

	protected ChangeSizeProjectComponent(Project project) {
		super(project);
	}

	@Override public void initComponent() {
	}

	@Override public void disposeComponent() {
		unchanged.clear();
		changeSizeByDocument.clear();
	}

	/**
	 * Can't use com.intellij.openapi.vcs.impl.LineStatusTrackerManager here because it only tracks changes for open files.
	 */
	public ChangeSize currentChangeListSizeInLines() {
		ChangeSize result = new ChangeSize(0);

		long startTime = System.currentTimeMillis();
		LocalChangeList changeList = ChangeListManager.getInstance(myProject).getDefaultChangeList();
		TextCompareProcessor compareProcessor = new TextCompareProcessor(
				ComparisonPolicy.TRIM_SPACE,
				DiffPolicy.LINES_WO_FORMATTING,
				HighlightMode.BY_LINE
		);

		for (Change change : changeList.getChanges()) {
			long duration = System.currentTimeMillis() - startTime;
			if (duration > diffDurationThresholdMillis) {
				result = new ChangeSize(result.value, true);
				break;
			}

			VirtualFile virtualFile = change.getVirtualFile();
			if (virtualFile == null) {
				result = result.add(currentChangeListSizeInLines(change, startTime, compareProcessor));
				continue;
			}
			final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
			if (document == null) {
				result = result.add(currentChangeListSizeInLines(change, startTime, compareProcessor));
				continue;
			}

			if (unchanged.contains(document)) {
				result = result.add(changeSizeByDocument.get(document));
			} else {
				ChangeSize changeSize = currentChangeListSizeInLines(change, startTime, compareProcessor);
				result = result.add(changeSize);

				if (!changeSize.isApproximate) {
					changeSizeByDocument.put(document, changeSize);
					unchanged.add(document);
					document.addDocumentListener(new DocumentListener() {
						@Override public void beforeDocumentChange(DocumentEvent event) { }

						@Override public void documentChanged(DocumentEvent event) {
							unchanged.remove(document);
							changeSizeByDocument.remove(document);
							document.removeDocumentListener(this);
						}
					}, myProject);
				}
			}
		}
		return result;
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
}
