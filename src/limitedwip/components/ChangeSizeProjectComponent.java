package limitedwip.components;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static limitedwip.components.VcsIdeUtil.durationThresholdMillis;

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
	}

	/**
	 * Can't use com.intellij.openapi.vcs.impl.LineStatusTrackerManager here because it only tracks changes for open files.
	 */
	public ChangeSize currentChangeListSizeInLines() {
		ChangeSize result = new ChangeSize(0);

		long startTime = System.currentTimeMillis();
		LocalChangeList changeList = ChangeListManager.getInstance(myProject).getDefaultChangeList();

		for (Change change : changeList.getChanges()) {
			long duration = System.currentTimeMillis() - startTime;
			if (duration > durationThresholdMillis) {
				result = new ChangeSize(result.value, true);
				break;
			}

			VirtualFile virtualFile = change.getVirtualFile();
			if (virtualFile == null) {
				result = result.add(VcsIdeUtil.currentChangeListSizeInLines(asList(change), startTime));
				continue;
			}
			final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
			if (document == null) {
				result = result.add(VcsIdeUtil.currentChangeListSizeInLines(asList(change), startTime));
				continue;
			}

			if (unchanged.contains(document)) {
				result = result.add(changeSizeByDocument.get(document));
			} else {
				ChangeSize changeSize = VcsIdeUtil.currentChangeListSizeInLines(asList(change), startTime);
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
}
