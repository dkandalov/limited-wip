package ru.autorevert.components;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.changes.actions.RefreshAction;
import com.intellij.openapi.vcs.changes.ui.CommitHelper;
import com.intellij.openapi.vcs.checkin.BaseCheckinHandlerFactory;
import com.intellij.openapi.vcs.checkin.BeforeCheckinDialogHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory;
import com.intellij.openapi.vcs.impl.CheckinHandlersManager;
import com.intellij.util.FunctionUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: dima
 * Date: 29/07/2012
 */
public class QuickCommitAction extends AnAction {

	private static final Pattern MESSAGE_PATTERN = Pattern.compile("^(.*?)(\\d+)$");

	@Override public void actionPerformed(final AnActionEvent event) {
		final Project project = getEventProject(event);

		if (anySystemCheckinHandlerCancelsCommit(project)) return;

		Runnable runnable = new Runnable() {
			@Override public void run() {
				RefreshAction.doRefresh(project);

				String lastCommitMessage = VcsConfiguration.getInstance(project).LAST_COMMIT_MESSAGE;
				String commitMessage = nextCommitMessage(lastCommitMessage);

				LocalChangeList defaultChangeList = ChangeListManager.getInstance(project).getDefaultChangeList();
				if (defaultChangeList.getChanges().isEmpty()) {
					Messages.showInfoMessage(project, VcsBundle.message("commit.dialog.no.changes.detected.text"),
							VcsBundle.message("commit.dialog.no.changes.detected.title"));
					return;
				}

				ArrayList<CheckinHandler> noCheckinHandlers = new ArrayList<CheckinHandler>();
				CommitHelper commitHelper = new CommitHelper(
						project,
						defaultChangeList,
						new ArrayList<Change>(defaultChangeList.getChanges()),
						"",
						commitMessage,
						noCheckinHandlers, true, true, FunctionUtil.nullConstant()
				);

				boolean committed = commitHelper.doCommit();
				if (committed) {
					VcsConfiguration.getInstance(project).saveCommitMessage(commitMessage);
				}
			}
		};
		ChangeListManager.getInstance(project).invokeAfterUpdate(
				runnable,
				InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE,
				"Refreshing changelists...",
				ModalityState.current()
		);
	}

	/**
	 * Couldn't find better way to "reuse" this code but to copy-paste it from
	 * {@link com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog#commit}.
	 */
	private static boolean anySystemCheckinHandlerCancelsCommit(Project project) {
		final AbstractVcs[] allActiveVcss = ProjectLevelVcsManager.getInstance(project).getAllActiveVcss();
		final List<VcsCheckinHandlerFactory> factoryList =
				CheckinHandlersManager.getInstance().getMatchingVcsFactories(Arrays.<AbstractVcs>asList(allActiveVcss));
		for (BaseCheckinHandlerFactory factory : factoryList) {
			final BeforeCheckinDialogHandler handler = factory.createSystemReadyHandler(project);
			if (handler != null) {
				if (! handler.beforeCommitDialogShownCallback(Collections.<CommitExecutor>emptyList(), false)) return true;
			}
		}
		return false;
	}

	public static String nextCommitMessage(@Nullable String lastCommitMessage) {
		if (lastCommitMessage == null) lastCommitMessage = "";

		if (endsWithDigits(lastCommitMessage)) {
			int thisCommitNumber = Integer.valueOf(extractTailDigitsFrom(lastCommitMessage)) + 1;
			return removeTailDigitsFrom(lastCommitMessage) + thisCommitNumber;
		} else {
			return lastCommitMessage + " 0";
		}
	}

	private static boolean endsWithDigits(String s) {
		return MESSAGE_PATTERN.matcher(s).matches();
	}

	private static String extractTailDigitsFrom(String s) {
		Matcher matcher = MESSAGE_PATTERN.matcher(s);
		if (!matcher.matches()) throw new IllegalStateException();

		return matcher.group(2);
	}

	private static String removeTailDigitsFrom(String s) {
		Matcher matcher = MESSAGE_PATTERN.matcher(s);
		if (!matcher.matches()) throw new IllegalStateException();

		return matcher.group(1);
	}
}
