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
package limitedwip.autorevert.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import limitedwip.autorevert.components.AutoRevertComponent

class StartOrStopAutoRevertAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val autoRevertComponent = project.getComponent(AutoRevertComponent::class.java) ?: return

        if (autoRevertComponent.isAutoRevertStarted) {
            autoRevertComponent.stopAutoRevert()
        } else {
            autoRevertComponent.startAutoRevert()
        }
    }

    override fun update(event: AnActionEvent) {
        val text = textFor(event.project)
        event.presentation.text = text
        event.presentation.description = text
        event.presentation.isEnabled = event.project != null
    }

    private fun textFor(project: Project?): String {
        if (project == null) return "Start auto-revert"
        val autoRevertComponent = project.getComponent(AutoRevertComponent::class.java) ?: return "Start auto-revert"

        return if (autoRevertComponent.isAutoRevertStarted) {
            "Stop auto-revert"
        } else {
            "Start auto-revert"
        }
    }
}
