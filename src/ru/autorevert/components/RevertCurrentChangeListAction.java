package ru.autorevert.components;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import ru.autorevert.IdeActions;


public class RevertCurrentChangeListAction extends AnAction {
	@Override public void actionPerformed(@NotNull AnActionEvent e) {
		new IdeActions(e.getProject()).revertCurrentChangeList();
	}
}
