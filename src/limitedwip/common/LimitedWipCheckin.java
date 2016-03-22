package limitedwip.common;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class LimitedWipCheckin extends CheckinHandlerFactory {
	private static final String EXTENSION_POINT_NAME = "LimitedWIP.checkinListener";

	@NotNull @Override public CheckinHandler createHandler(@NotNull final CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
		return new CheckinHandler() {
			@Override public void checkinSuccessful() {
				Project project = panel.getProject();

				ChangeListManager changeListManager = ChangeListManager.getInstance(project);
				int uncommittedFileCount = changeListManager.getDefaultChangeList().getChanges().size() - panel.getSelectedChanges().size();
				boolean allFileAreCommitted = uncommittedFileCount == 0;

				notifySettingsListeners(allFileAreCommitted);
			}
		};
	}

	private void notifySettingsListeners(boolean allFileAreCommitted) {
		final ExtensionPoint<Listener> extensionPoint = Extensions.getRootArea().getExtensionPoint(EXTENSION_POINT_NAME);
		for (Listener listener : extensionPoint.getExtensions()) {
			listener.onSuccessfulCheckin(allFileAreCommitted);
		}
	}

	public static void registerListener(Disposable disposable, final Listener listener) {
		final ExtensionPoint<Listener> extensionPoint = Extensions.getRootArea().getExtensionPoint(EXTENSION_POINT_NAME);
		extensionPoint.registerExtension(listener);
		Disposer.register(disposable, new Disposable() {
			@Override public void dispose() {
				extensionPoint.unregisterExtension(listener);
			}
		});
	}

	public interface Listener {
		void onSuccessfulCheckin(boolean allFileAreCommitted);
	}
}
