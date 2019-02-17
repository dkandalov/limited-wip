package limitedwip.watchdog

import limitedwip.shouldEqual
import org.junit.Test

class PathMatcherTests {
    @Test fun `file name`() {
        PathMatcher.parse("*.approved").matches("foo.approved") shouldEqual true
        PathMatcher.parse("*.approved").matches("bar.approved") shouldEqual true
        PathMatcher.parse("*.approved").matches("/some/path/bar.approved") shouldEqual true
        PathMatcher.parse("*.approved").matches("foo.kt") shouldEqual false
    }

    @Test fun `dir name`() {
        PathMatcher.parse("/some/path/*.approved").matches("/some/path/foo.approved") shouldEqual true
        PathMatcher.parse("/some/path/*.approved").matches("/some/other/path/foo.approved") shouldEqual false
        PathMatcher.parse("/some/**/path/*.approved").matches("/some/other/path/foo.approved") shouldEqual true
        PathMatcher.parse("/some/**/path/*.approved").matches("/some/other/longer/path/foo.approved") shouldEqual true
        PathMatcher.parse("/some/**/path/*.approved").matches("/a/different/path/foo.approved") shouldEqual false
    }
}