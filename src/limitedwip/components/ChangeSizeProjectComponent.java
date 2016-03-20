package limitedwip.components;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;

public class ChangeSizeProjectComponent extends AbstractProjectComponent {
	protected ChangeSizeProjectComponent(Project project) {
		super(project);
	}

	@Override public void initComponent() {
		super.initComponent();
	}

	@Override public void disposeComponent() {
		super.disposeComponent();
	}

	public ChangeSize currentChangeListSizeInLines() {
		// TODO implement
		return new ChangeSize(0);
	}

	public static ChangeSizeProjectComponent getInstance(Project project) {
		return project.getComponent(ChangeSizeProjectComponent.class);
	}
}
