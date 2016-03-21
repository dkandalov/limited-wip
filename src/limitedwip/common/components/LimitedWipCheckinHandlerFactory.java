package limitedwip.common.components;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import limitedwip.autorevert.components.AutoRevertComponent;
import limitedwip.watchdog.components.WatchdogComponent;
import org.jetbrains.annotations.NotNull;

public class LimitedWipCheckinHandlerFactory extends CheckinHandlerFactory {
	@NotNull @Override public CheckinHandler createHandler(@NotNull final CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
		return new CheckinHandler() {
			@Override public void checkinSuccessful() {
				Project project = panel.getProject();

				ChangeListManager changeListManager = ChangeListManager.getInstance(project);
				int uncommittedFilesSize = changeListManager.getDefaultChangeList().getChanges().size() - panel.getSelectedChanges().size();

				AutoRevertComponent autoRevertComponent = project.getComponent(AutoRevertComponent.class);
				autoRevertComponent.onVcsCommit(uncommittedFilesSize);

				WatchdogComponent watchdogComponent = project.getComponent(WatchdogComponent.class);
				watchdogComponent.onVcsCommit();
			}
		};
	}
}
