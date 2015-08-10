package limitedwip.components;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class CheckinHandlerFactory extends VcsCheckinHandlerFactory {
    protected CheckinHandlerFactory(@NotNull VcsKey key) {
        super(key);
    }

    @NotNull @Override protected CheckinHandler createVcsHandler(final CheckinProjectPanel panel) {
        return new CheckinHandler() {
            @Override public void checkinSuccessful() {
                ChangeListManager changeListManager = ChangeListManager.getInstance(panel.getProject());
                int uncommittedFilesSize = changeListManager.getDefaultChangeList().getChanges().size() - panel.getSelectedChanges().size();

                LimitedWIPProjectComponent projectComponent = panel.getProject().getComponent(LimitedWIPProjectComponent.class);
                projectComponent.onVcsCommit(uncommittedFilesSize);
            }
        };
    }
}
