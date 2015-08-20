package limitedwip.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import limitedwip.components.LimitedWIPProjectComponent;
import limitedwip.components.VcsIdeUtil.ChangeSize;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;

public class WatchdogStatusBarWidget implements StatusBarWidget {
    private static final String textPrefix = "Change size: ";

    private String text = "";

    @Override public void install(@NotNull StatusBar statusBar) {
    }

    @Override public void dispose() {
    }

    public void showChangeSize(ChangeSize linesInChange, int maxLinesInChange) {
        if (linesInChange.timedOut) {
            text = textPrefix + ">=" + linesInChange.value + "/" + maxLinesInChange;
        } else {
            text = textPrefix + linesInChange.value + "/" + maxLinesInChange;
        }
    }

    public void showInitialText(int maxLinesInChange) {
        text = textPrefix + "-/" + maxLinesInChange;
    }

    @Override public WidgetPresentation getPresentation(@NotNull PlatformType type) {
        return new TextPresentation() {
            @NotNull @Override public String getText() {
                return text;
            }

            @NotNull @Override public String getMaxPossibleText() {
                return textPrefix + "999/999";
            }

            @Override public String getTooltipText() {
                return "Shows amount of changed lines in current change list vs change size limit.";
            }

            @Override public Consumer<MouseEvent> getClickConsumer() {
                return new Consumer<MouseEvent>() {
                    @Override public void consume(MouseEvent mouseEvent) {
                        DataContext dataContext = DataManager.getInstance().getDataContext(mouseEvent.getComponent());
                        Project project = PlatformDataKeys.PROJECT.getData(dataContext);
                        if (project == null) return;

                        LimitedWIPProjectComponent limitedWIPProjectComponent = project.getComponent(LimitedWIPProjectComponent.class);
                        limitedWIPProjectComponent.skipNotificationsUntilCommit(false);
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
