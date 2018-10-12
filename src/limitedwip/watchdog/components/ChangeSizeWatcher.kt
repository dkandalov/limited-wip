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
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.FakeRevision
import com.intellij.util.diff.FilesTooBigForDiffException
import limitedwip.watchdog.ChangeSize
import java.util.*

class ChangeSizeWatcher(private val project: Project) {
    
    private val changeSizeCache = ChangeSizeCache(project)
    private var changeSize = ChangeSize(0, true)
    @Volatile private var isRunningBackgroundDiff: Boolean = false

    fun currentChangeListSizeInLines() = changeSize

    fun onTimer() {
        calculateCurrentChangeListSizeInLines()
    }

    /**
     * Can't use com.intellij.openapi.vcs.impl.LineStatusTrackerManager here because it only tracks changes for open files.
     */
    private fun calculateCurrentChangeListSizeInLines() {
        if (isRunningBackgroundDiff) return
        val application = ApplicationManager.getApplication()

        val (newChangeSize, changesToDiff) = application.runReadAction(Computable<Pair<ChangeSize, List<Change>>> {
            val changeList = ChangeListManager.getInstance(project).defaultChangeList

            val changesToDiff = ArrayList<Change>()
            var result = ChangeSize(0)
            for (change in changeList.changes) {
                val document = change.document()
                val changeSize = changeSizeCache[document]
                if (changeSize == null) {
                    changesToDiff.add(change)
                } else {
                    result = result.add(changeSize)
                }
            }
            Pair(result, changesToDiff)
        })
        if (changesToDiff.isEmpty()) {
            changeSize = newChangeSize
            return
        }

        application.executeOnPooledThread {
            isRunningBackgroundDiff = true

            val comparisonManager = ComparisonManager.getInstance()
            val changeSizeByChange = HashMap<Change, ChangeSize>()
            for (change in changesToDiff) {
                changeSizeByChange[change] = currentChangeListSizeInLines(change, comparisonManager)
            }

            application.invokeLater {
                changeSize = newChangeSize
                for (it in changeSizeByChange.values) {
                    changeSize = changeSize.add(it)
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
        val virtualFile = virtualFile
        return if (virtualFile == null) null
        else FileDocumentManager.getInstance().getDocument(virtualFile)
    }

    private fun currentChangeListSizeInLines(change: Change, comparisonManager: ComparisonManager): ChangeSize {
        return try {
            calculateChangeSize(change, comparisonManager)
        } catch (ignored: VcsException) {
            ChangeSize(0, true)
        } catch (ignored: FilesTooBigForDiffException) {
            ChangeSize(0, true)
        }
    }

    private fun calculateChangeSize(change: Change, comparisonManager: ComparisonManager): ChangeSize {
        val beforeRevision = change.beforeRevision
        val afterRevision = change.afterRevision
        if (beforeRevision is FakeRevision || afterRevision is FakeRevision) {
            return ChangeSize(0, true)
        }

        val revision = afterRevision ?: beforeRevision
        if (revision == null || revision.file.fileType.isBinary) return ChangeSize(0)

        val contentBefore = if (beforeRevision != null) beforeRevision.content ?: "" else ""
        val contentAfter = if (afterRevision != null) afterRevision.content ?: "" else ""

        val result = comparisonManager
            .compareWords(contentBefore, contentAfter, IGNORE_WHITESPACES, EmptyProgressIndicator())
            .sumBy { fragment ->
                val changeSize1 = fragment.endOffset1 - fragment.startOffset1
                val changeSize2 = fragment.endOffset2 - fragment.startOffset2
                // Use changeSize2 unless it's deleted code, then use changeSize1.
                if (changeSize2 != 0) changeSize2 else changeSize1
            }
        return ChangeSize(result)
    }


    private class ChangeSizeCache(private val parentDisposable: Disposable) {
        private val changeSizeByDocument = WeakHashMap<Document, ChangeSize>()

        operator fun set(document: Document, changeSize: ChangeSize) {
            changeSizeByDocument[document] = changeSize
            document.addDocumentListener(object: DocumentListener {
                override fun beforeDocumentChange(event: DocumentEvent) {}
                override fun documentChanged(event: DocumentEvent) {
                    changeSizeByDocument.remove(document)
                    document.removeDocumentListener(this)
                }
            }, parentDisposable)
        }

        operator fun get(document: Document?): ChangeSize? {
            return if (document == null) null else changeSizeByDocument[document]
        }
    }
}
