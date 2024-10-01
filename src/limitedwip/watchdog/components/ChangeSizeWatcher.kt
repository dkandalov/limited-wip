package limitedwip.watchdog.components

import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy.IGNORE_WHITESPACES
import com.intellij.diff.fragments.LineFragment
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
    private val documentChangeSizeCache = DocumentChangeSizeCache(project)
    private var changeSizesWithPath = ChangeSizesWithPath.empty
    @Volatile private var isRunningBackgroundDiff: Boolean = false

    private val comparisonManager = ComparisonManager.getInstance()
    private val application = ApplicationManager.getApplication()
    private val projectBasePath = project.basePath ?: ""

    val changeListSizeInLines: ChangeSizesWithPath get() = changeSizesWithPath

    /**
     * Can't use com.intellij.openapi.vcs.impl.LineStatusTrackerManager here because it only tracks changes for open files.
     */
    fun calculateCurrentChangeListSizeInLines() {
        if (isRunningBackgroundDiff) return
        val changeList = project.defaultChangeList()
        if (changeList == null) {
            changeSizesWithPath = ChangeSizesWithPath.empty
            return
        }

        val (newChangeSizesWithPath, changesToProcess) = application.runReadAction(Computable {
            var result = ChangeSizesWithPath.empty
            val changesToDiff = ArrayList<Change>()
            changeList.changes.forEach { change ->
                val changeSize = documentChangeSizeCache[change.document()]
                if (changeSize == null) {
                    changesToDiff.add(change)
                } else {
                    result = result.add(change.path.replace(projectBasePath, ""), changeSize)
                }
            }
            Pair(result, changesToDiff)
        })
        if (changesToProcess.isEmpty()) {
            changeSizesWithPath = newChangeSizesWithPath
            return
        }

        application.executeOnPooledThread {
            isRunningBackgroundDiff = true

            val changeSizeByChange = changesToProcess.associateWith { change ->
                comparisonManager.calculateChangeSizeInLines(change)
            }

            application.invokeLater {
                changeSizesWithPath = newChangeSizesWithPath
                changeSizeByChange.forEach { (change, changeSize) ->
                    changeSizesWithPath = changeSizesWithPath.add(change.path, changeSize)
                }
                changeSizeByChange.forEach { (change, changeSize) ->
                    val document = change.document()
                    if (document != null && !changeSize.isApproximate) {
                        documentChangeSizeCache[document] = changeSize
                    }
                }
                isRunningBackgroundDiff = false
            }
        }
    }

    private fun Change.document(): Document? {
        return FileDocumentManager.getInstance().getDocument(virtualFile ?: return null)
    }

    private class DocumentChangeSizeCache(private val parentDisposable: Disposable) {
        private val changeSizeByDocument = WeakHashMap<Document, ChangeSize>()

        operator fun set(document: Document, changeSize: ChangeSize) {
            changeSizeByDocument[document] = changeSize
            document.addDocumentListener(
                object : DocumentListener {
                    override fun documentChanged(event: DocumentEvent) {
                        changeSizeByDocument.remove(document)
                        document.removeDocumentListener(this)
                    }
                },
                parentDisposable
            )
        }

        operator fun get(document: Document?): ChangeSize? =
            if (document == null) null else changeSizeByDocument[document]
    }
}

private val Change.path: String
    get() = beforeRevision?.file?.path ?: afterRevision?.file?.path ?: ""

fun ComparisonManager.calculateChangeSizeInLines(change: Change): ChangeSize =
    try {
        doCalculateChangeSizeInLines(change) { textBefore, textAfter ->
            compareLinesInner(textBefore, textAfter, IGNORE_WHITESPACES, EmptyProgressIndicator())
        }
    } catch (ignored: VcsException) {
        ChangeSize.approximatelyEmpty
    } catch (ignored: FilesTooBigForDiffException) {
        ChangeSize.approximatelyEmpty
    }

private fun doCalculateChangeSizeInLines(change: Change, compare: (String, String) -> List<LineFragment>): ChangeSize {
    val beforeRevision = change.beforeRevision
    val afterRevision = change.afterRevision
    if (beforeRevision is FakeRevision || afterRevision is FakeRevision) {
        return ChangeSize.approximatelyEmpty
    }

    val revision = afterRevision ?: beforeRevision
    if (revision == null || revision.file.fileType.isBinary) return ChangeSize.empty

    val contentBefore = (beforeRevision?.content ?: "").replace("\r\n", "\n")
    val contentAfter = (afterRevision?.content ?: "").replace("\r\n", "\n")

    val result = compare(contentBefore, contentAfter).sumOf { fragment ->
        val changeSizeAfter = contentAfter.calculateChangeSize(IntRange(fragment.startOffset2, fragment.endOffset2 - 1))
        if (changeSizeAfter != 0) changeSizeAfter // Use changeSizeAfter unless all the code has been deleted.
        else contentBefore.calculateChangeSize(IntRange(fragment.startOffset1, fragment.endOffset1 - 1))
    }
    return ChangeSize(result)
}

private val newLineRegex = Regex("\n+")

private fun String.calculateChangeSize(range: IntRange): Int {
    val subSequence = subSequence(range).replace(newLineRegex, "\n").trim()
    return subSequence.count { it == '\n' } + (if (subSequence.isEmpty()) 0 else 1)
}
