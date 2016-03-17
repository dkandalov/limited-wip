package limitedwip.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import limitedwip.components.LimitedWIPProjectComponent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;

public class AutoRevertStatusBarWidget implements StatusBarWidget {
    private static final String timeTillRevertText = "Auto-revert in ";
    private static final String startedText = "Auto-revert started";
    private static final String stoppedText = "Auto-revert stopped";

    private String text = "";

    @Override public void install(@NotNull StatusBar statusBar) {
    }

    @Override public void dispose() {
    }

    public void showTime(String timeLeft) {
        text = timeTillRevertText + timeLeft;
    }

    public void showStartedText() {
        text = startedText;
    }

    public void showStoppedText() {
        text = stoppedText;
    }

    @Override public WidgetPresentation getPresentation(@NotNull PlatformType type) {
        return new TextPresentation() {
            @NotNull @Override public String getText() {
                return text;
            }

            @Deprecated @NotNull @Override public String getMaxPossibleText() {
                return "";
            }

            @Override public String getTooltipText() {
                return "Click to start/stop auto-revert";
            }

            @Override public Consumer<MouseEvent> getClickConsumer() {
                return new Consumer<MouseEvent>() {
                    @Override public void consume(MouseEvent mouseEvent) {
                        DataContext dataContext = DataManager.getInstance().getDataContext(mouseEvent.getComponent());
                        Project project = PlatformDataKeys.PROJECT.getData(dataContext);
                        if (project == null) return;

                        LimitedWIPProjectComponent limitedWIPProjectComponent = project.getComponent(LimitedWIPProjectComponent.class);
                        if (limitedWIPProjectComponent.isAutoRevertStarted()) {
                            limitedWIPProjectComponent.stopAutoRevert();
                        } else {
                            limitedWIPProjectComponent.startAutoRevert();
                        }
                    }
                };
            }

            @Override public float getAlignment() {
                return Component.CENTER_ALIGNMENT;
            }
        };
    }

    @NotNull @Override public String ID() {
        return "LimitedWIP_" + this.getClass().getSimpleName();
    }
}
