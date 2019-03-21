package limitedwip.watchdog.components

import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy.IGNORE_WHITESPACES
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.FakeRevision
import com.intellij.util.diff.FilesTooBigForDiffException
import limitedwip.common.vcs.defaultChangeList
import limitedwip.watchdog.ChangeSize
import limitedwip.watchdog.ChangeSizesWithPath
import java.util.*

class ChangeSizeWatcher(private val project: Project) {
    private val changeSizeCache = ChangeSizeCache(project)
    private var changeSize = ChangeSize.NA
    private var changeSizesWithPath = ChangeSizesWithPath.empty
    @Volatile private var isRunningBackgroundDiff: Boolean = false

    private val comparisonManager = ComparisonManager.getInstance()
    private val application = ApplicationManager.getApplication()

    val changeListSizeInLines get() = changeSizesWithPath

    /**
     * Can't use com.intellij.openapi.vcs.impl.LineStatusTrackerManager here because it only tracks changes for open files.
     */
    fun calculateCurrentChangeListSizeInLines() {
        if (isRunningBackgroundDiff) return
        val changeList = project.defaultChangeList()
        if (changeList == null) {
            changeSize = ChangeSize.NA
            changeSizesWithPath = ChangeSizesWithPath.empty
            return
        }

        val (newChangeSize, newChangeSizesWithPath, changesToDiff) = application.runReadAction(Computable {
            var result = ChangeSize.empty
            var result2 = ChangeSizesWithPath.empty
            val changesToDiff = ArrayList<Change>()
            for (change in changeList.changes) {
                val changeSize = changeSizeCache[change.document()]
                if (changeSize == null) {
                    changesToDiff.add(change)
                } else {
                    result += changeSize
                    result2 = result2.add(change.path.replace(project.basePath ?: "", ""), changeSize)
                }
            }
            Triple(result, result2, changesToDiff)
        })
        if (changesToDiff.isEmpty()) {
            changeSize = newChangeSize
            changeSizesWithPath = newChangeSizesWithPath
            return
        }

        application.executeOnPooledThread {
            isRunningBackgroundDiff = true

            val changeSizeByChange = HashMap<Change, ChangeSize>()
            for (change in changesToDiff) {
                changeSizeByChange[change] = calculateChangeSizeInLines(change, comparisonManager)
            }

            application.invokeLater {
                changeSize = newChangeSize
                changeSizesWithPath = newChangeSizesWithPath
                for ((change, it) in changeSizeByChange) {
                    changeSize += it
                    changeSizesWithPath = changeSizesWithPath.add(change.path, it)
                }
                for ((change, changeSize) in changeSizeByChange) {
                    val document = change.document()
                    if (document != null && !changeSize.isApproximate) {
                        changeSizeCache[document] = changeSize
                    }
                }
                isRunningBackgroundDiff = false
            }
        }
    }

    private fun Change.document(): Document? {
        val file = virtualFile ?: return null
        return FileDocumentManager.getInstance().getDocument(file)
    }

    private class ChangeSizeCache(private val parentDisposable: Disposable) {
        private val changeSizeByDocument = WeakHashMap<Document, ChangeSize>()

        operator fun set(document: Document, changeSize: ChangeSize) {
            changeSizeByDocument[document] = changeSize
            document.addDocumentListener(object : DocumentListener {
                override fun beforeDocumentChange(event: DocumentEvent) {}
                override fun documentChanged(event: DocumentEvent) {
                    changeSizeByDocument.remove(document)
                    document.removeDocumentListener(this)
                }
            }, parentDisposable)
        }

        operator fun get(document: Document?): ChangeSize? =
            if (document == null) null else changeSizeByDocument[document]
    }
}

private val Change.path: String
    get() = beforeRevision?.file?.path ?: afterRevision?.file?.path ?: ""


fun calculateChangeSizeInLines(change: Change, comparisonManager: ComparisonManager): ChangeSize =
    try {
        doCalculateChangeSizeInLines(change, comparisonManager)
    } catch (ignored: VcsException) {
        ChangeSize.approximatelyEmpty
    } catch (ignored: FilesTooBigForDiffException) {
        ChangeSize.approximatelyEmpty
    }

private fun doCalculateChangeSizeInLines(change: Change, comparisonManager: ComparisonManager): ChangeSize {
    val beforeRevision = change.beforeRevision
    val afterRevision = change.afterRevision
    if (beforeRevision is FakeRevision || afterRevision is FakeRevision) {
        return ChangeSize.approximatelyEmpty
    }

    val revision = afterRevision ?: beforeRevision
    if (revision == null || revision.file.fileType.isBinary) return ChangeSize.empty

    val contentBefore = if (beforeRevision != null) beforeRevision.content ?: "" else ""
    val contentAfter = if (afterRevision != null) afterRevision.content ?: "" else ""

    val result = comparisonManager
        .compareWords(contentBefore, contentAfter, IGNORE_WHITESPACES, EmptyProgressIndicator())
        .sumBy { fragment ->
            val subSequence1 = contentBefore.subSequence(IntRange(fragment.startOffset1, fragment.endOffset1 - 1)).replace(Regex("\n+"), "\n")
            val subSequence2 = contentAfter.subSequence(IntRange(fragment.startOffset2, fragment.endOffset2 - 1)).replace(Regex("\n+"), "\n")

            val changeSize1 = subSequence1.count { it == '\n' } + (if (subSequence1.isEmpty()) 0 else 1)
            val changeSize2 = subSequence2.count { it == '\n' } + (if (subSequence2.isEmpty()) 0 else 1)

            // Use changeSize2 unless it's deleted code, then use changeSize1.
            if (changeSize2 != 0) changeSize2 else changeSize1
        }
    return ChangeSize(result)
}
