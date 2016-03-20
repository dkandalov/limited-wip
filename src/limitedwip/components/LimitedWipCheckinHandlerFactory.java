package limitedwip.components;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class LimitedWipCheckinHandlerFactory extends CheckinHandlerFactory {
	@NotNull @Override public CheckinHandler createHandler(@NotNull final CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
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
