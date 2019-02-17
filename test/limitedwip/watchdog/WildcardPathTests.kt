package limitedwip.watchdog

import limitedwip.shouldEqual
import org.junit.Test

class WildcardPathTests {
    @Test fun `file name`() {
        val (fileName, dir, srcRoot) = convertToRegexp("*.approved")
        Regex(fileName.pattern).matches("foo.approved") shouldEqual true
        Regex(fileName.pattern).matches("foo.kt") shouldEqual false
        (dir == null) shouldEqual true
        (srcRoot == null) shouldEqual true
    }
}