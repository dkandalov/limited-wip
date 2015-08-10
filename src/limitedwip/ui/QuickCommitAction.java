/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package limitedwip.ui;

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
import com.intellij.openapi.vcs.impl.CheckinHandlersManager;
import com.intellij.util.FunctionUtil;
import limitedwip.components.LimitedWIPProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class QuickCommitAction extends AnAction {

	private static final Pattern MESSAGE_PATTERN = Pattern.compile("^(.*?)(\\d+)$");

	@Override public void actionPerformed(@NotNull final AnActionEvent event) {
		final Project project = getEventProject(event);
		if (project == null) return;

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
						noCheckinHandlers, true, true, FunctionUtil.nullConstant(),
						new CommitResultHandler() {
							@Override public void onSuccess(@NotNull String commitMessage) {}
							@Override public void onFailure() {}
						}
				);

				boolean committed = commitHelper.doCommit();
				if (committed) {
					VcsConfiguration.getInstance(project).saveCommitMessage(commitMessage);
					LimitedWIPProjectComponent projectComponent = project.getComponent(LimitedWIPProjectComponent.class);
					projectComponent.onQuickCommit();
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
	 * {@link com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog}.
	 */
	private static boolean anySystemCheckinHandlerCancelsCommit(Project project) {
		final AbstractVcs[] allActiveVcss = ProjectLevelVcsManager.getInstance(project).getAllActiveVcss();
		final List<BaseCheckinHandlerFactory> factoryList =
				CheckinHandlersManager.getInstance().getRegisteredCheckinHandlerFactories(allActiveVcss);
		for (BaseCheckinHandlerFactory factory : factoryList) {
			BeforeCheckinDialogHandler handler = factory.createSystemReadyHandler(project);
			if (handler != null && !handler.beforeCommitDialogShown(project, new ArrayList<Change>(), new ArrayList<CommitExecutor>(), false)) {
				return true;
			}
		}
		return false;
	}

	static String nextCommitMessage(@Nullable String lastCommitMessage) {
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
