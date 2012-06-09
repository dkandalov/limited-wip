package ru.groovy

import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.vcs.impl.CheckinHandlersManager
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.unscramble.AnalyzeStacktraceUtil
import com.intellij.unscramble.UnscrambleDialog
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.ui.ChangesListView
import com.intellij.openapi.vcs.changes.actions.RollbackDeletionAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.ChangesUtil
import org.jetbrains.annotations.Nullable
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.rollback.RollbackEnvironment
import com.intellij.openapi.vcs.changes.ui.RollbackProgressModifier
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.vcs.AbstractVcsHelper
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager

class Rollback {
	public void actionPerformed(AnActionEvent e) {
		Project project = e.getData(PlatformDataKeys.PROJECT);
		if (project == null) {
			return;
		}
		final String title = ActionPlaces.CHANGES_VIEW_TOOLBAR.equals(e.getPlace()) ? null : "Can not rollback now";
		if (ChangeListManager.getInstance(project).isFreezedWithNotification(title)) return;
		FileDocumentManager.getInstance().saveAllDocuments();

		List<FilePath> missingFiles = e.getData(ChangesListView.MISSING_FILES_DATA_KEY);
		if (missingFiles != null && !missingFiles.isEmpty()) {
			new RollbackDeletionAction().actionPerformed(e);
		}

		List<VirtualFile> modifiedWithoutEditing = getModifiedWithoutEditing(e);
		if (modifiedWithoutEditing != null && !modifiedWithoutEditing.isEmpty()) {
			rollbackModifiedWithoutEditing(project, modifiedWithoutEditing);
		}

		Change[] changes = getChanges(project, e);
		if (changes != null) {
			new RollbackWorker(project, true).doRollback(changes.toList(), true, null, null)
		}
	}

	private static class ChangesCheckHelper {
		private Change[] myChanges;
		private final boolean myChangesSet;

		public ChangesCheckHelper(final Project project, final AnActionEvent e) {
			Change[] changes = e.getData(VcsDataKeys.CHANGES);
			if (changes == null) {
				final VirtualFile[] files = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
				if (files != null) {
					final ChangeListManager clManager = ChangeListManager.getInstance(project);
					final List<Change> changesList = new LinkedList<Change>();
					for (VirtualFile vf : files) {
						changesList.addAll(clManager.getChangesIn(vf));
					}
					if (! changesList.isEmpty()) {
						changes = changesList.toArray(new Change[changesList.size()]);
					}
				}
			}
			myChangesSet = changes != null && changes.length > 0;
			if (myChangesSet) {
				if (ChangesUtil.allChangesInOneListOrWholeListsSelected(project, changes)) {
					myChanges = changes;
				}
			}
		}

		public boolean isChangesSet() {
			return myChangesSet;
		}

		public Change[] getChanges() {
			return myChanges;
		}
	}

	@Nullable
	private static Change[] getChanges(final Project project, final AnActionEvent e) {
		final ChangesCheckHelper helper = new ChangesCheckHelper(project, e);
		if (helper.isChangesSet()) return helper.getChanges();

		final VirtualFile[] virtualFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
		if (virtualFiles != null && virtualFiles.length > 0) {
			List<Change> result = new ArrayList<Change>();
			for(VirtualFile file: virtualFiles) {
				result.addAll(ChangeListManager.getInstance(project).getChangesIn(file));
			}
			return result.toArray(new Change[result.size()]);
		}
		return null;
	}

	@Nullable
	private static List<VirtualFile> getModifiedWithoutEditing(final AnActionEvent e) {
		final List<VirtualFile> modifiedWithoutEditing = e.getData(VcsDataKeys.MODIFIED_WITHOUT_EDITING_DATA_KEY);
		if (modifiedWithoutEditing != null && modifiedWithoutEditing.size() > 0) {
			return modifiedWithoutEditing;
		}
		return null;
	}

	private static void rollbackModifiedWithoutEditing(final Project project, final List<VirtualFile> modifiedWithoutEditing) {
		String message = (modifiedWithoutEditing.size() == 1) ?
			VcsBundle.message("rollback.modified.without.editing.confirm.single", modifiedWithoutEditing.get(0).getPresentableUrl()) :
			VcsBundle.message("rollback.modified.without.editing.confirm.multiple", modifiedWithoutEditing.size());
		int rc = Messages.showYesNoDialog(project, message, VcsBundle.message("changes.action.rollback.title"), Messages.getQuestionIcon());
		if (rc != 0) {
			return;
		}
		final List<VcsException> exceptions = new ArrayList<VcsException>();

		final ProgressManager progressManager = ProgressManager.getInstance();
		final Runnable action = new Runnable() {
			public void run() {
				final ProgressIndicator indicator = progressManager.getProgressIndicator();
				try {
					ChangesUtil.processVirtualFilesByVcs(project, modifiedWithoutEditing, new ChangesUtil.PerVcsProcessor<VirtualFile>() {
						public void process(final AbstractVcs vcs, final List<VirtualFile> items) {
							final RollbackEnvironment rollbackEnvironment = vcs.getRollbackEnvironment();
							if (rollbackEnvironment != null) {
								if (indicator != null) {
									indicator.setText(vcs.getDisplayName() + ": performing rollback...");
									indicator.setIndeterminate(false);
								}
								rollbackEnvironment.rollbackModifiedWithoutCheckout(items, exceptions, new RollbackProgressModifier(items.size(), indicator));
								if (indicator != null) {
									indicator.setText2("");
								}
							}
						}
					});
				}
				catch (ProcessCanceledException e) {
					// for files refresh
				}
				if (!exceptions.isEmpty()) {
					AbstractVcsHelper.getInstance(project).showErrors(exceptions, VcsBundle.message("rollback.modified.without.checkout.error.tab"));
				}
				VirtualFileManager.getInstance().refresh(true, new Runnable() {
					public void run() {
						for(VirtualFile virtualFile: modifiedWithoutEditing) {
							VcsDirtyScopeManager.getInstance(project).fileDirty(virtualFile);
						}
					}
				});
			}
		};
		progressManager.runProcessWithProgressSynchronously(action, VcsBundle.message("changes.action.rollback.text"), true, project);
	}
}

static registerInMetaClasses(AnActionEvent anActionEvent) {
	[Object.metaClass, Class.metaClass].each {
		it.actionEvent = { anActionEvent }
		it.project = { actionEvent().getData(PlatformDataKeys.PROJECT) }
		it.editor = { actionEvent().getData(PlatformDataKeys.EDITOR) }
		it.fileText = { actionEvent().getData(PlatformDataKeys.FILE_TEXT) }
	}
}

static showPopup(String htmlBody, String toolWindowId = ToolWindowId.RUN, MessageType messageType = MessageType.INFO) {
	ToolWindowManager.getInstance(project()).notifyByBalloon(toolWindowId, messageType, htmlBody)
}

static showPopup(Project project, htmlBody, String toolWindowId = ToolWindowId.RUN, MessageType messageType = MessageType.INFO) {
	ToolWindowManager.getInstance(project).notifyByBalloon(toolWindowId, messageType, htmlBody)
}

static showInUnscrambleDialog(Exception e) {
	def writer = new StringWriter()
	e.printStackTrace(new PrintWriter(writer))
	def s = UnscrambleDialog.normalizeText(writer.buffer.toString())
	def console = UnscrambleDialog.addConsole(project(), [])
	AnalyzeStacktraceUtil.printStacktrace(console, s)
}

static catchingAll(Closure closure) {
	try {
		closure.call()
	} catch (Exception e) {
		showInUnscrambleDialog(e)
		showPopup("Caught exception", ToolWindowId.RUN, MessageType.ERROR)
	}
}


registerInMetaClasses(actionEvent)

def registerAction(actionId, String keyStroke = "", Closure closure) {
	def actionManager = ActionManager.instance
	if (actionManager.getActionIds("").toList().contains(actionId)) {
		actionManager.unregisterAction(actionId)
	}

	def action = new AnAction() {
		@Override
		void actionPerformed(AnActionEvent e) {
			closure.call(e)
		}
	}
	KeymapManager.instance.activeKeymap.addShortcut(actionId, new KeyboardShortcut(KeyStroke.getKeyStroke(keyStroke), null))
	actionManager.registerAction(actionId, action)
}

registerAction("myAction3", "alt shift H") { AnActionEvent event ->
//		showPopup(event.project, "myAction3")
	ToolWindowManager.getInstance(event.project).notifyByBalloon(ToolWindowId.RUN, MessageType.INFO, "myAction3")
}

registerAction("myAction", "alt shift V") { AnActionEvent event ->
	catchingAll {
		def changeList = ChangeListManager.getInstance(event.project).defaultChangeList

		new RollbackWorker(event.project, true).doRollback(changeList.changes.toList(), true, null, null)
		showPopup(event.project, changeList.name + " " + changeList.changes)
	}
}

class MyHandlerFactory extends CheckinHandlerFactory {
	@Override CheckinHandler createHandler(CheckinProjectPanel panel, CommitContext commitContext) {
		new CheckinHandler() {
			@Override void checkinSuccessful() {
				ChangeListManager.getInstance(panel.project).with {
					def uncommittedSize = defaultChangeList.changes.size() - panel.selectedChanges.size()

					SwingUtilities.invokeLater {
						ToolWindowManager.getInstance(panel.project).notifyByBalloon(ToolWindowId.RUN, MessageType.INFO,
								"uncommittedSize: " + uncommittedSize)
					}
				}
			}
		}
	}
}
registerAction("myAction2", "alt shift B") { event ->
	catchingAll {
		def factories = CheckinHandlersManager.instance.getRegisteredCheckinHandlerFactories()
		factories.findAll {it.class.name == MyHandlerFactory.class.name}.each {
			CheckinHandlersManager.instance.unregisterCheckinHandlerFactory(it)
		}
		CheckinHandlersManager.instance.registerCheckinHandlerFactory(new MyHandlerFactory())

		showPopup("factories: " + factories)
	}
}

showPopup("evaled plugin")
