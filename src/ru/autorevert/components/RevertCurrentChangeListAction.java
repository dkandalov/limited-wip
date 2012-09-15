package ru.autorevert.components;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import ru.autorevert.IdeActions;

/**
 * User: dima
 * Date: 15/09/2012
 */
public class RevertCurrentChangeListAction extends AnAction {
	@Override public void actionPerformed(AnActionEvent e) {
		new IdeActions(e.getProject()).revertCurrentChangeList();
	}
}
