package limitedwip.watchdog

import com.intellij.diff.comparison.ComparisonManagerImpl
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.SimpleContentRevision
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import limitedwip.watchdog.components.calculateChangeSize
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class CalculateChangeSizeTests : LightPlatformCodeInsightFixtureTestCase() {
    private val comparisonManager = ComparisonManagerImpl()

    @Test fun `test change size calculation`() {
        assertThat(calculateChangeSize("", ""), equalTo(ChangeSize(0)))
        assertThat(calculateChangeSize("a", ""), equalTo(ChangeSize(1)))
        assertThat(calculateChangeSize("", "a"), equalTo(ChangeSize(1)))
        assertThat(calculateChangeSize("a", "a"), equalTo(ChangeSize(0)))

        assertThat(calculateChangeSize("text\n\n\n", ""), equalTo(ChangeSize(4)))
        assertThat(calculateChangeSize("text      ", ""), equalTo(ChangeSize(4)))

        // Ignores trailing newlines, but unfortunately doesn't ignore newlines in the middle.
        assertThat(calculateChangeSize("text\n\n\ntext\n\n\n", "\n"), equalTo(ChangeSize(11)))
    }

    private fun calculateChangeSize(before: String, after: String): ChangeSize {
        val change = Change(
            SimpleContentRevision(before, LocalFilePath("before.txt", false), "some-revision-before"),
            SimpleContentRevision(after, LocalFilePath("after.txt", false), "some-revision-after")
        )
        return calculateChangeSize(change, comparisonManager)
    }
}