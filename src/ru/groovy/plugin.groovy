package ru.groovy

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
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
		changeList.changes.each { FileDocumentManager.instance.reloadFiles(it.virtualFile) }

		showPopup(event.project, "!" + changeList.name + " " + changeList.changes)
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
