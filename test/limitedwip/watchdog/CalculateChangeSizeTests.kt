package limitedwip.watchdog

import com.intellij.diff.comparison.ComparisonManagerImpl
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.SimpleContentRevision
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import limitedwip.watchdog.components.calculateChangeSizeInLines
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class CalculateChangeSizeTests : LightPlatformCodeInsightFixtureTestCase() {
    private val comparisonManager = ComparisonManagerImpl()

    @Test fun `test trivial diffs`() {
        assertThat(calculateChangeSizeInLines("", ""), equalTo(ChangeSize(0)))
        assertThat(calculateChangeSizeInLines("text", ""), equalTo(ChangeSize(1)))
        assertThat(calculateChangeSizeInLines("", "text"), equalTo(ChangeSize(1)))
        assertThat(calculateChangeSizeInLines("text", "text"), equalTo(ChangeSize(0)))
    }

    @Test fun `test change size calculation`() {
        assertThat(calculateChangeSizeInLines("text\ntext", "text"), equalTo(ChangeSize(1)))
        assertThat(calculateChangeSizeInLines("text\ntext", ""), equalTo(ChangeSize(2)))
    }

    @Test fun `test ignore spaces and newlines`() {
        assertThat(calculateChangeSizeInLines("\n\ntext\n\n", ""), equalTo(ChangeSize(1)))
        assertThat(calculateChangeSizeInLines("    text    ", ""), equalTo(ChangeSize(1)))
        assertThat(calculateChangeSizeInLines("text\n\n\ntext\n\n\n", "text"), equalTo(ChangeSize(1)))
        assertThat(calculateChangeSizeInLines("text\n\n\ntext\n\n\n", "\n"), equalTo(ChangeSize(2)))
    }

    private fun calculateChangeSize(before: String, after: String): ChangeSize {
        val change = Change(
            SimpleContentRevision(before, LocalFilePath("before.txt", false), "some-revision-before"),
            SimpleContentRevision(after, LocalFilePath("after.txt", false), "some-revision-after")
        )
        return calculateChangeSizeInLines(change, comparisonManager)
    }
}