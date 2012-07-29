package ru.autorevert.components;

import com.intellij.notification.*;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.BeforeCheckinDialogHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory;
import com.intellij.openapi.vcs.impl.CheckinHandlersManager;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.util.Function;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import ru.autorevert.settings.Settings;

import javax.swing.event.HyperlinkEvent;
import java.lang.reflect.Field;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * User: dima
 * Date: 29/07/2012
 */
public class DisableCommitsWithErrorsComponent implements ApplicationComponent, Settings.Listener {
	private static final Logger LOG = Logger.getInstance(DisableCommitsWithErrorsComponent.class);

	private final Ref<Boolean> enabled = Ref.create(false);

	@SuppressWarnings("unchecked")
	@Override public void initComponent() {

		// This is a hack caused by limitations of IntelliJ API.
		//  - cannot use CheckinHandlerFactory because its createSystemReadyHandler() is never called
		//  - cannot use VcsCheckinHandlerFactory through extension points because need to register
		//    checkin handler for all VCSs available
		//  - cannot use CheckinHandlersManager#registerCheckinHandlerFactory() because it doesn't properly
		//    register VcsCheckinHandlerFactory
		//
		// Therefore, this hack with reflection.

		accessField(CheckinHandlersManager.getInstance(), asList("b", "myVcsMap"), new Function<MultiMap, Void>() {
			@Override public Void fun(MultiMap multiMap) {

				for (Object key : multiMap.keySet()) {
					multiMap.putValue(key, new ProhibitingCheckinHandlerFactory((VcsKey) key, enabled));
				}

				return null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static void accessField(Object o, List<String> possibleFieldNames, Function function) {
		for (Field field : o.getClass().getDeclaredFields()) {
			if (possibleFieldNames.contains(field.getName())) {
				field.setAccessible(true);
				try {

					function.fun(field.get(o));
					return;

				} catch (Exception e) {
					LOG.warn("Error while initializing ProhibitingCheckinHandlerFactory");
				}
			}
		}
	}

	@Override public void disposeComponent() {
	}

	@NotNull @Override public String getComponentName() {
		return "DisableCommitsWithErrorsComponent";
	}

	@Override public void onNewSettings(Settings settings) {
		enabled.set(settings.disableCommitsWithErrors);
	}

	private static class ProhibitingCheckinHandlerFactory extends VcsCheckinHandlerFactory {
		private final Ref<Boolean> enabled;

		protected ProhibitingCheckinHandlerFactory(@NotNull VcsKey key, Ref<Boolean> enabled) {
			super(key);
			this.enabled = enabled;
		}

		@Override public BeforeCheckinDialogHandler createSystemReadyHandler(final Project project) {
			return new BeforeCheckinDialogHandler() {
				@Override
				public boolean beforeCommitDialogShownCallback(Iterable<CommitExecutor> commitExecutors, boolean b) {
					if (!enabled.get()) return true;

					WolfTheProblemSolver wolf = WolfTheProblemSolver.getInstance(project);
					for (Module module : ModuleManager.getInstance(project).getModules()) {
						if (wolf.hasProblemFilesBeneath(module)) {
							notifyThatCommitWasCancelled();
							return false;
						}
					}
					return true;
				}
			};
		}

		private void notifyThatCommitWasCancelled() {
			NotificationListener listener = new NotificationListener() {
				@Override
				public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
					ShowSettingsUtil.getInstance().showSettingsDialog((Project) null, AutoRevertAppComponent.class);
				}
			};

			Notifications notificationsManager = (Notifications) NotificationsManager.getNotificationsManager();
			notificationsManager.notify(new Notification(
					AutoRevertAppComponent.DISPLAY_NAME,
					"Commit was cancelled because project has errors",
					"(You can disable it <a href=\"\">here</a>)",
					NotificationType.WARNING,
					listener
			));
		}

		@NotNull @Override protected CheckinHandler createVcsHandler(CheckinProjectPanel panel) {
			return new MyDummyHandler();
		}

		private static class MyDummyHandler extends CheckinHandler {}

	}

}
