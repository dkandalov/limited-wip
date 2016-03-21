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
package limitedwip.watchdog.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;

public class IdeActions {
	private final Project project;
    private ChangeSize lastChangeSize = new ChangeSize(0);
    private int skipChecks;

    public IdeActions(Project project) {
		this.project = project;
	}

	public ChangeSize currentChangeListSizeInLines() {
        if (skipChecks > 0) {
            skipChecks--;
            return lastChangeSize;
        }
        ChangeSize changeSize = ApplicationManager.getApplication().runReadAction(new Computable<ChangeSize>() {
            @Override public ChangeSize compute() {
                return ChangeSizeProjectComponent.getInstance(project).currentChangeListSizeInLines();
            }
        });
        if (changeSize.isApproximate) {
            changeSize = new ChangeSize(lastChangeSize.value, true);
            skipChecks = 10;
        }
        lastChangeSize = changeSize;
        return changeSize;
	}
}
