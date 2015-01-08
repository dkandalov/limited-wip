package limitedwip.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import limitedwip.IdeActions;


public class RevertCurrentChangeListAction extends AnAction {
	@Override public void actionPerformed(@NotNull AnActionEvent e) {
		new IdeActions(e.getProject()).revertCurrentChangeList();
	}
}
