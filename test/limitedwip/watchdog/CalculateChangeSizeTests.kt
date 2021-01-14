package limitedwip.watchdog

import com.intellij.diff.comparison.ComparisonManagerImpl
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.SimpleContentRevision
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import limitedwip.shouldEqual
import limitedwip.watchdog.components.calculateChangeSizeInLines
import org.junit.Test

class CalculateChangeSizeTests : BasePlatformTestCase() {
    private val comparisonManager = ComparisonManagerImpl()

    @Test fun `test trivial diffs`() {
        changeSizeInLines("", "") shouldEqual ChangeSize(0)
        changeSizeInLines("text", "") shouldEqual ChangeSize(1)
        changeSizeInLines("", "text") shouldEqual ChangeSize(1)
        changeSizeInLines("text", "text") shouldEqual ChangeSize(0)
    }

    @Test fun `test change size calculation`() {
        changeSizeInLines("line1\nline2", "") shouldEqual ChangeSize(2)
        changeSizeInLines("line1\nline2", "line1") shouldEqual ChangeSize(1)
        changeSizeInLines("line1\nline2", "line2") shouldEqual ChangeSize(1)

        changeSizeInLines("line1\nline2\nline3", "line1") shouldEqual ChangeSize(2)
        changeSizeInLines("line1\nline2\nline3", "line2") shouldEqual ChangeSize(2)
        changeSizeInLines("line1\nline2\nline3", "line3") shouldEqual ChangeSize(2)
        changeSizeInLines("line1\nline2\nline3", "line1\nline2") shouldEqual ChangeSize(1)
        changeSizeInLines("line1\nline2\nline3", "line1\nline3") shouldEqual ChangeSize(1)
        changeSizeInLines("line1\nline2\nline3", "line2\nline3") shouldEqual ChangeSize(1)
    }

    // This test replicates actual behaviour on Windows when line separators in VCS use \r\n
    // but the latest revision seems normalised by IJ to use \n.
    @Test fun `test change size calculation with mixed separators`() {
        changeSizeInLines("line1\r\nline2\r\nline3", "line1\nline2") shouldEqual ChangeSize(1)
        changeSizeInLines("line1\r\nline2\r\nline3", "line1\nline3") shouldEqual ChangeSize(1)
        changeSizeInLines("line1\r\nline2\r\nline3", "line2\nline3") shouldEqual ChangeSize(1)
    }

    @Test fun `test ignore spaces and newlines`() {
        changeSizeInLines("\n\nline1\n\n", "") shouldEqual ChangeSize(1)
        changeSizeInLines("    line1    ", "") shouldEqual ChangeSize(1)
        changeSizeInLines("line1\n\n\nline2\n\n\n", "line1") shouldEqual ChangeSize(1)
        changeSizeInLines("line1\n\n\nline2\n\n\n", "line2") shouldEqual ChangeSize(1)
        changeSizeInLines("line1\n\n\nline2\n\n\n", "\n") shouldEqual ChangeSize(2)
    }

    @Test fun `test correctly diff a bit of code`() {
        changeSizeInLines(
            """
            |try {
            |} catch (Error error) {
            |}
            """.trimMargin(),
            """
            |try {
            |}
            """.trimMargin()
        ) shouldEqual ChangeSize(1)
    }

    private fun changeSizeInLines(before: String, after: String) =
        comparisonManager.calculateChangeSizeInLines(Change(
            SimpleContentRevision(before, LocalFilePath("before.txt", false), "some-revision-before"),
            SimpleContentRevision(after, LocalFilePath("after.txt", false), "some-revision-after")
        ))
}