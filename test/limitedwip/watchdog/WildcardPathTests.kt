package limitedwip.watchdog

import limitedwip.shouldEqual
import org.junit.Test

class WildcardPathTests {
    @Test fun `file name`() {
        convertToRegexp("*.approved").matches("foo.approved") shouldEqual true
        convertToRegexp("*.approved").matches("bar.approved") shouldEqual true
        convertToRegexp("*.approved").matches("/some/path/bar.approved") shouldEqual true
        convertToRegexp("*.approved").matches("foo.kt") shouldEqual false
    }

    @Test fun `dir name`() {
        convertToRegexp("/some/path/*.approved").matches("/some/path/foo.approved") shouldEqual true
        convertToRegexp("/some/path/*.approved").matches("/some/other/path/foo.approved") shouldEqual false
        convertToRegexp("/some/**/path/*.approved").matches("/some/other/path/foo.approved") shouldEqual true
    }
}