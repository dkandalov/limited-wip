package limitedwip.limbo

import limitedwip.shouldEqual
import limitedwip.verify
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times

class LimboTests {
    private val ide = mock(Ide::class.java)
    private val limbo = Limbo(ide, Limbo.Settings(enabled = true))

    @Test fun `allow commits only after running a unit test`() {
        limbo.isCommitAllowed() shouldEqual false
        ide.verify(times(1)).notifyThatCommitWasCancelled()
        limbo.onUnitTestSucceeded()
        limbo.isCommitAllowed() shouldEqual true
    }

    @Test fun `after commit need to run a unit test to be able to commit again`() {
        limbo.onUnitTestSucceeded()
        limbo.isCommitAllowed() shouldEqual true

        limbo.onSuccessfulCommit()
        limbo.isCommitAllowed() shouldEqual false

        limbo.onUnitTestSucceeded()
        limbo.isCommitAllowed() shouldEqual true
    }

    @Test fun `revert changes on failed unit test`() {
        limbo.onUnitTestFailed()
        ide.verify(times(1)).revertCurrentChangeList()
    }

    @Test fun `can do one-off commit without running a unit test`() {
        limbo.isCommitAllowed() shouldEqual false
        limbo.allowOneCommitWithoutChecks()
        limbo.isCommitAllowed() shouldEqual true
        limbo.onSuccessfulCommit()
        limbo.isCommitAllowed() shouldEqual false
    }
}