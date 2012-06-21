package ru.autorevert.groovy_playground

import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.unscramble.AnalyzeStacktraceUtil
import com.intellij.unscramble.UnscrambleDialog
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.KeyStroke
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.wm.*

/*
 * This groovy script was used to explore how IntelliJ API works.
 * It's not required by the rest of the project. It left here as a reference.
 *
 * This script is only useful with another (unpublished) plugin which
 * reevaluates groovy scripts in IntelliJ at runtime making feedback loop shorter.
 */

static registerInMetaClasses(AnActionEvent anActionEvent) {
	[Object.metaClass, Class.metaClass].each {
		it.actionEvent = { anActionEvent }
		it.project = { actionEvent().getData(PlatformDataKeys.PROJECT) }
		it.editor = { actionEvent().getData(PlatformDataKeys.EDITOR) }
		it.fileText = { actionEvent().getData(PlatformDataKeys.FILE_TEXT) }
	}
}

static showPopup(String htmlBody, String toolWindowId = ToolWindowId.RUN, MessageType messageType = MessageType.INFO) {
	((Notifications) NotificationsManager.notificationsManager).notify(new Notification("aaa", "bbb", "ccc", NotificationType.INFORMATION))
//	ToolWindowManager.getInstance(project()).notifyByBalloon(toolWindowId, messageType, htmlBody)
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


registerAction("myAction3", "alt shift B") { event ->
	catchingAll {
		StatusBar statusBar = null

		def widget = new StatusBarWidget() {
			@Override StatusBarWidget.WidgetPresentation getPresentation(StatusBarWidget.PlatformType type) {
				new StatusBarWidget.TextPresentation() {
					@Override String getText() { "Auto-revert in 12:48" }

					@Override String getMaxPossibleText() { "Auto-revert in 12:48" }

					@Override float getAlignment() { Component.CENTER_ALIGNMENT; }

					@Override String getTooltipText() { "Auto-revert in 12:48" }

					@Override Consumer<MouseEvent> getClickConsumer() {
						new Consumer<MouseEvent>() {
							@Override void consume(MouseEvent mouseEvent) {
								catchingAll {
									DataContext dataContext = DataManager.getInstance().getDataContext((Component) statusBar);
									Project project = PlatformDataKeys.PROJECT.getData(dataContext);
									showPopup(project, "ooooonnnn")
								}
							}
						}
					}
				}
			}

			@Override void install(StatusBar sb) { this.statusBar = sb }

			@Override void dispose() { statusBar = null }

			@Override String ID() { "myStatusBarWidget" }
		}
		WindowManager.instance.getStatusBar(event.project).with {
			removeWidget(widget.ID())
			addWidget(widget)
			updateWidget(widget.ID())
		}

		showPopup(event.project, "aaaaa2")
//		def indicator = ProgressManager.getInstance().getProgressIndicator()
//		indicator.start()
//		indicator.setFraction(0.3)
	}
}

showPopup("evaled plugin")
