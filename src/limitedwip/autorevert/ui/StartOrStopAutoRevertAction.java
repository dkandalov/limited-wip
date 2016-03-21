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
package limitedwip.autorevert.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import limitedwip.autorevert.components.AutoRevertComponent;
import org.jetbrains.annotations.NotNull;

public class StartOrStopAutoRevertAction extends AnAction {
	@Override public void actionPerformed(@NotNull AnActionEvent event) {
		Project project = event.getProject();
		if (project == null) return;

		AutoRevertComponent autoRevertComponent = project.getComponent(AutoRevertComponent.class);
		if (autoRevertComponent.isAutoRevertStarted()) {
			autoRevertComponent.stopAutoRevert();
		} else {
			autoRevertComponent.startAutoRevert();
		}
	}

	@Override public void update(@NotNull AnActionEvent event) {
		String text = textFor(event.getProject());
		event.getPresentation().setText(text);
		event.getPresentation().setDescription(text);
		event.getPresentation().setEnabled(event.getProject() != null);
	}

	private static String textFor(Project project) {
		if (project == null) return "Start auto-revert";

		if (project.getComponent(AutoRevertComponent.class).isAutoRevertStarted()) {
			return "Stop auto-revert";
		} else {
			return "Start auto-revert";
		}
	}
}
