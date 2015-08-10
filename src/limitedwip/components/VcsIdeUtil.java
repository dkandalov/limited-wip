package limitedwip.components;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.impl.ComparisonPolicy;
import com.intellij.openapi.diff.impl.fragments.LineFragment;
import com.intellij.openapi.diff.impl.processing.DiffPolicy;
import com.intellij.openapi.diff.impl.processing.HighlightMode;
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.checkin.BeforeCheckinDialogHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory;
import com.intellij.openapi.vcs.impl.CheckinHandlersManager;
import com.intellij.util.Function;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.diff.FilesTooBigForDiffException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*;
import static java.util.Arrays.asList;

public class VcsIdeUtil {
    private static final Logger log = Logger.getInstance(VcsIdeUtil.class);

    public static int currentChangeListSizeInLines(Collection<Change> changes) {
        int result = 0;
        TextCompareProcessor compareProcessor = new TextCompareProcessor(
                ComparisonPolicy.TRIM_SPACE,
                DiffPolicy.LINES_WO_FORMATTING,
                HighlightMode.BY_LINE
        );

        for (Change change : changes) {
            try {

                result += amountOfChangedLinesIn(change, compareProcessor);

            } catch (VcsException ignored) {
            } catch (FilesTooBigForDiffException ignored) {
            }
        }
        return result;
    }

    private static int amountOfChangedLinesIn(Change change, TextCompareProcessor compareProcessor) throws VcsException, FilesTooBigForDiffException {
        ContentRevision beforeRevision = change.getBeforeRevision();
        ContentRevision afterRevision = change.getAfterRevision();

        ContentRevision revision = afterRevision;
        if (revision == null) revision = beforeRevision;
        if (revision == null || revision.getFile().getFileType().isBinary()) return 0;

        String contentBefore = beforeRevision != null ? beforeRevision.getContent() : "";
        String contentAfter = afterRevision != null ? afterRevision.getContent() : "";
        if (contentBefore == null) contentBefore = "";
        if (contentAfter == null) contentAfter = "";

        int result = 0;
        for (LineFragment fragment : compareProcessor.process(contentBefore, contentAfter)) {
            if (fragment.getType() == DELETED) {
                result += fragment.getModifiedLines1();
            } else if (fragment.getType() == CHANGED || fragment.getType() == CONFLICT || fragment.getType() == INSERT) {
                result += fragment.getModifiedLines2();
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static void registerBeforeCheckInListener(final CheckinListener listener) {
        // This is a hack caused by limitations of IntelliJ API.
        //  - cannot use CheckinHandlerFactory because:
        //		- CheckinHandler is used just before commit (and after displaying commit dialog)
        // 		- its CheckinHandlerFactory#createSystemReadyHandler() doesn't seem to be called
        //  - cannot use VcsCheckinHandlerFactory through extension points because need to register
        //    checkin handler for all VCSs available
        //  - cannot use CheckinHandlersManager#registerCheckinHandlerFactory() because it doesn't properly
        //    register VcsCheckinHandlerFactory
        //
        // Therefore, using reflection.

        accessField(CheckinHandlersManager.getInstance(), asList("b", "myVcsMap"), new Function<MultiMap, Void>() {
            @Override public Void fun(MultiMap multiMap) {

                for (Object key : multiMap.keySet()) {
                    multiMap.putValue(key, new DelegatingCheckinHandlerFactory((VcsKey) key, listener));
                }

                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void accessField(Object object, List<String> possibleFieldNames, Function function) {
        for (Field field : object.getClass().getDeclaredFields()) {
            if (possibleFieldNames.contains(field.getName())) {
                field.setAccessible(true);
                try {

                    function.fun(field.get(object));
                    return;

                } catch (Exception ignored) {
                }
            }
        }
        log.warn("Failed to access fields: " + possibleFieldNames + " on '" + object + "'");
    }


    public interface CheckinListener {
        boolean allowCheckIn(@NotNull Project project, @NotNull List<Change> changes);
    }


    private static class DelegatingCheckinHandlerFactory extends VcsCheckinHandlerFactory {
        private final CheckinListener listener;

        protected DelegatingCheckinHandlerFactory(@NotNull VcsKey key, CheckinListener listener) {
            super(key);
            this.listener = listener;
        }

        @Override public BeforeCheckinDialogHandler createSystemReadyHandler(@NotNull final Project project) {
            return new BeforeCheckinDialogHandler() {
                @Override public boolean beforeCommitDialogShown(@NotNull Project project, @NotNull List<Change> changes,
                                                                 @NotNull Iterable<CommitExecutor> executors, boolean showVcsCommit) {
                    return listener.allowCheckIn(project, changes);
                }
            };
        }

        @NotNull @Override protected CheckinHandler createVcsHandler(CheckinProjectPanel panel) {
            return new CheckinHandler() {};
        }
    }
}
