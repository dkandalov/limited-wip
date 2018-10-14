package limitedwip.autorevert.ui

import limitedwip.autorevert.ui.QuickCommitAction.Companion.nextCommitMessage
import limitedwip.shouldEqual
import org.junit.Test

class QuickCommitActionTests {

    @Test fun `come up with next commit message`() {
        nextCommitMessage(null) shouldEqual " 0"
        nextCommitMessage("") shouldEqual " 0"

        nextCommitMessage(" 0") shouldEqual " 1"
        nextCommitMessage(" 1") shouldEqual " 2"

        nextCommitMessage("some message") shouldEqual "some message 0"
        nextCommitMessage("some message 0") shouldEqual "some message 1"
        nextCommitMessage("some message 9") shouldEqual "some message 10"
        nextCommitMessage("some message 10") shouldEqual "some message 11"
        nextCommitMessage("some message 19") shouldEqual "some message 20"
    }
}
