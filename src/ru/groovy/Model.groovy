package ru.groovy

import com.intellij.openapi.project.Project
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindowManager

/**
 * User: dima
 * Date: 10/06/2012
 */
class Model {
	boolean started = false
	int counter
	Project project

	Model(Project project) {
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
