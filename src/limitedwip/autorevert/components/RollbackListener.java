package limitedwip.autorevert.components;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.InvokeAfterUpdateMode;
import kotlin.Unit;

import java.util.Collection;
import java.util.function.Function;

/**
 * This class is written in Java because in the latest version IJ API forces not nullable AnActionEvents in callbacks.
 * However, they can actually be null in older versions.
 * I.e. API changes force you to compile code which doesn't work in older versions of IJ.
 */
public class RollbackListener {
    private final Project project;
    private final Function<Boolean, Unit> onRollback;
    private AnActionListener actionListener;

    public RollbackListener(Project project, Function<Boolean, Unit> onRollback) {
        this.project = project;
        this.onRollback = onRollback;
    }

    public void enable() {
        if (actionListener != null) return;

        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        actionListener = new AnActionListener() {
            @Override public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
            }

            @Override public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                if (event == null) return;
                if (!action.getClass().getSimpleName().equals("RollbackAction")) return;

                // Note that checking changelist size immediately after rollback action will show changes as they were before the rollback
                // (even if the check is scheduled to be run later on EDT).
                // The following seems to be the only reliable way to do it.
                Runnable afterUpdate = () -> {
                    Collection<Change> changes = changeListManager.getDefaultChangeList().getChanges();
                    onRollback.apply(changes.isEmpty());
                };
                changeListManager.invokeAfterUpdate(afterUpdate, InvokeAfterUpdateMode.SILENT_CALLBACK_POOLED, null, ModalityState.any());
            }
        };
        ActionManager.getInstance().addAnActionListener(actionListener, project);
    }

    public void disable() {
        if (actionListener == null) return;

        ActionManager.getInstance().removeAnActionListener(actionListener);
        actionListener = null;
    }
}
