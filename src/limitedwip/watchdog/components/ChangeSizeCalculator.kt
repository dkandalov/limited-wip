package limitedwip.watchdog.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diff.impl.ComparisonPolicy
import com.intellij.openapi.diff.impl.processing.DiffPolicy
import com.intellij.openapi.diff.impl.processing.HighlightMode
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.FakeRevision
import com.intellij.util.diff.FilesTooBigForDiffException
import limitedwip.common.PluginId
import limitedwip.watchdog.ChangeSize
import java.util.*

class ChangeSizeCalculator(private val project: Project) {
    private val changeSizeCache = ChangeSizeCache()
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

        val pair = ApplicationManager.getApplication().runReadAction(Computable<Pair<ChangeSize, List<Change>>> {
            val changeList = ChangeListManager.getInstance(project).defaultChangeList
            val changesToDiff = ArrayList<Change>()

            var result = ChangeSize(0)
            for (change in changeList.changes) {
                val document = getDocumentFor(change)
                val changeSize = changeSizeCache[document]
                if (changeSize == null) {
                    changesToDiff.add(change)
                } else {
                    result = result.add(changeSize)
                }
            }
            Pair.create(result, changesToDiff)
        })
        if (pair.second.isEmpty()) {
            changeSize = pair.first
            return
        }

        Thread(Runnable {
            isRunningBackgroundDiff = true

            val compareProcessor = TextCompareProcessor(
                ComparisonPolicy.TRIM_SPACE,
                DiffPolicy.LINES_WO_FORMATTING,
                HighlightMode.BY_LINE
            )
            val changeSizeByChange = HashMap<Change, ChangeSize>()
            for (change in pair.second) {
                changeSizeByChange[change] = currentChangeListSizeInLines(change, compareProcessor)
            }

            ApplicationManager.getApplication().invokeLater {
                changeSize = pair.first
                for (it in changeSizeByChange.values) {
                    changeSize = changeSize.add(it)
                }
                for ((key, value) in changeSizeByChange) {
                    val document = getDocumentFor(key)
                    if (document != null && !value.isApproximate) {
                        changeSizeCache.put(document, value)
                    }
                }
                isRunningBackgroundDiff = false
            }
        }, PluginId.value + "-DiffThread").start()
    }

    private fun getDocumentFor(change: Change): Document? {
        val virtualFile = change.virtualFile
        return if (virtualFile == null) null else FileDocumentManager.getInstance().getDocument(virtualFile)
    }

    private fun currentChangeListSizeInLines(change: Change, compareProcessor: TextCompareProcessor): ChangeSize {
        return try {
            amountOfChangedLinesIn(change, compareProcessor)
        } catch (ignored: VcsException) {
            ChangeSize(0, true)
        } catch (ignored: FilesTooBigForDiffException) {
            ChangeSize(0, true)
        }
    }

    private fun amountOfChangedLinesIn(change: Change, compareProcessor: TextCompareProcessor): ChangeSize {
        val beforeRevision = change.beforeRevision
        val afterRevision = change.afterRevision
        if (beforeRevision is FakeRevision || afterRevision is FakeRevision) {
            return ChangeSize(0, true)
        }

        var revision = afterRevision
        if (revision == null) revision = beforeRevision
        if (revision == null || revision.file.fileType.isBinary) return ChangeSize(0)

        var contentBefore: String? = if (beforeRevision != null) beforeRevision.content else ""
        var contentAfter: String? = if (afterRevision != null) afterRevision.content else ""
        if (contentBefore == null) contentBefore = ""
        if (contentAfter == null) contentAfter = ""

        var result = 0
        for (fragment in compareProcessor.process(contentBefore, contentAfter)) {
            if (fragment.type == DELETED) {
                result += fragment.modifiedLines1
            } else if (fragment.type == CHANGED || fragment.type == CONFLICT || fragment.type == INSERT) {
                result += fragment.modifiedLines2
            }
        }
        return ChangeSize(result)
    }


    private class ChangeSizeCache {
        private val changeSizeByDocument = HashMap<Document, ChangeSize>()

        fun put(document: Document?, changeSize: ChangeSize) {
            if (document == null) return
            changeSizeByDocument[document] = changeSize
            document.addDocumentListener(object : DocumentListener {
                override fun beforeDocumentChange(event: DocumentEvent) {}
                override fun documentChanged(event: DocumentEvent) {
                    changeSizeByDocument.remove(document)
                    document.removeDocumentListener(this)
                }
            })
        }

        operator fun get(document: Document?): ChangeSize? {
            return if (document == null) null else changeSizeByDocument[document]
        }
    }
}
