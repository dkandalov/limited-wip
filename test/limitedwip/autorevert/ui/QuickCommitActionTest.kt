package limitedwip.autorevert.ui

import org.junit.Test

import limitedwip.autorevert.ui.QuickCommitAction.Companion.nextCommitMessage
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat

class QuickCommitActionTest {

    @Test fun `come up with next commit message`() {
        assertThat(nextCommitMessage(null), equalTo(" 0"))
        assertThat(nextCommitMessage(""), equalTo(" 0"))

        assertThat(nextCommitMessage(" 0"), equalTo(" 1"))
        assertThat(nextCommitMessage(" 1"), equalTo(" 2"))

        assertThat(nextCommitMessage("some message"), equalTo("some message 0"))
        assertThat(nextCommitMessage("some message 0"), equalTo("some message 1"))
        assertThat(nextCommitMessage("some message 9"), equalTo("some message 10"))
        assertThat(nextCommitMessage("some message 10"), equalTo("some message 11"))
        assertThat(nextCommitMessage("some message 19"), equalTo("some message 20"))
    }
}
