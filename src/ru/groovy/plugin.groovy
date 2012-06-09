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
import java.util.concurrent.TimeUnit
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

static showPopup(Project project, String htmlBody, String toolWindowId = ToolWindowId.RUN, MessageType messageType = MessageType.INFO) {
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

// TODO
//((ComponentManagerEx)project()).registerComponent((ComponentConfig) new ComponentConfig().with {
//	implementationClass = RevertComponent.class
//	it
//})

class RevertComp {
	boolean started = false
	int counter
	Project project

	RevertComp(Project project) {
		this.project = project
	}

	def synchronized start() {
		started = true
		counter = 0
	}

	def synchronized stop() {
		started = false
	}

	def synchronized isStarted() {
		started
	}

	def synchronized onTimer() {
		if (!started) return

		counter++
		showPopup(project, "" + counter)

		if (counter % TimeUnit.MINUTES.toSeconds(2) == 0) {
			revertChanges()
			counter = 0
		}
	}

	def synchronized onCommit() {
		counter = 0
	}

	private def revertChanges() {
		SwingUtilities.invokeAndWait() {
			def changeList = ChangeListManager.getInstance(project).defaultChangeList
			new RollbackWorker(project, true).doRollback(changeList.changes.toList(), true, null, null)
			changeList.changes.each { FileDocumentManager.instance.reloadFiles(it.virtualFile) }

			showPopup(project, "!" + changeList.name + " " + changeList.changes)
		}
	}

	private static showPopup(Project project, String htmlBody, String toolWindowId = ToolWindowId.RUN, MessageType messageType = MessageType.INFO) {
		SwingUtilities.invokeLater() {
			ToolWindowManager.getInstance(project).notifyByBalloon(toolWindowId, messageType, htmlBody)
		}
	}
}
def comp = new RevertComp(project())

registerAction("revertStart", "alt shift G") { AnActionEvent event ->
	if (comp.started) {
		comp.stop()
	} else {
		comp.start()
	}
}

registerAction("myAction3", "alt shift H") { AnActionEvent event ->
	// start timer
	new Timer(true).schedule(new TimerTask() {
		@Override void run() {
			comp.onTimer()
		}
	}, 0, 1000)

	// register commit callback
	def factories = CheckinHandlersManager.instance.getRegisteredCheckinHandlerFactories()
	factories.findAll {it.class.name == MyHandlerFactory.class.name}.each {
		CheckinHandlersManager.instance.unregisterCheckinHandlerFactory(it)
	}
	CheckinHandlersManager.instance.registerCheckinHandlerFactory(new MyHandlerFactory({
		comp.onCommit()
	}))

	showPopup(event.project, "initialized RevertComp")
}

class MyHandlerFactory extends CheckinHandlerFactory {
	Closure callback

	MyHandlerFactory(Closure callback) {
		this.callback = callback
	}

	@Override CheckinHandler createHandler(CheckinProjectPanel panel, CommitContext commitContext) {
		new CheckinHandler() {
			@Override void checkinSuccessful() {
				ChangeListManager.getInstance(panel.project).with {
					def uncommittedSize = defaultChangeList.changes.size() - panel.selectedChanges.size()
					if (uncommittedSize == 0) {
						callback.call()
					}
				}
			}
		}
	}
}

def revertActiveChangeList = { Project project ->
	catchingAll {
		def changeList = ChangeListManager.getInstance(project).defaultChangeList
		new RollbackWorker(project, true).doRollback(changeList.changes.toList(), true, null, null)
		changeList.changes.each { FileDocumentManager.instance.reloadFiles(it.virtualFile) }

		showPopup(project, "!" + changeList.name + " " + changeList.changes)
	}
}

registerAction("myAction", "alt shift V") { event -> revertActiveChangeList(event.project) }

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
