package limitedwip.limbo

import limitedwip.expect
import limitedwip.limbo.components.Ide
import limitedwip.shouldEqual
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never

class LimboTests {
    private val ide = mock(Ide::class.java)
    private val settings = Limbo.Settings(
        enabled = true,
        notifyOnRevert = true,
        openCommitDialogOnPassedTest = true
    )
    private val limbo = Limbo(ide, settings)

    @Test fun `allow commits only after running a unit test`() {
        limbo.isCommitAllowed() shouldEqual false
        ide.expect().notifyThatCommitWasCancelled()

        limbo.onUnitTestSucceeded()
        limbo.isCommitAllowed() shouldEqual true
    }

    @Test fun `show commit dialog after successful test run`() {
        limbo.onUnitTestSucceeded()
        ide.expect().openCommitDialog()
    }

    @Test fun `after commit need to run a unit test to be able to commit again`() {
        limbo.onUnitTestSucceeded()
        limbo.isCommitAllowed() shouldEqual true

        limbo.onSuccessfulCommit()
        limbo.isCommitAllowed() shouldEqual false

        limbo.onUnitTestSucceeded()
        limbo.isCommitAllowed() shouldEqual true
    }

    @Test fun `don't allow commits if files were changed after running a unit test`() {
        limbo.onUnitTestSucceeded()
        limbo.isCommitAllowed() shouldEqual true

        limbo.onFileChange()
        limbo.isCommitAllowed() shouldEqual false
    }

    @Test fun `revert changes on failed unit test`() {
        limbo.onUnitTestFailed()
        ide.expect().revertCurrentChangeList()
    }

    @Test fun `notify user on revert`() {
        `revert changes on failed unit test`()
        ide.expect().notifyThatChangesWereReverted()
    }

    @Test fun `don't notify user on revert if notification is disabled in settings`() {
        limbo.onSettings(settings.copy(notifyOnRevert = false))
        `revert changes on failed unit test`()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `can do one-off commit without running a unit test`() {
        limbo.isCommitAllowed() shouldEqual false
        limbo.forceOneCommit()
        limbo.isCommitAllowed() shouldEqual true
        ide.expect().openCommitDialog()

        limbo.onSuccessfulCommit()
        limbo.isCommitAllowed() shouldEqual false
    }

    @Test fun `if disabled, always allow commits`() {
        limbo.onSettings(settings.copy(enabled = false))
        limbo.isCommitAllowed() shouldEqual true
    }

    @Test fun `if disabled, don't revert changes on failed unit test`() {
        limbo.onSettings(settings.copy(enabled = false))
        limbo.onUnitTestFailed()
        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `if disabled, don't show commit dialog and don't count successful test runs`() {
        limbo.onSettings(settings.copy(enabled = false))
        limbo.onUnitTestSucceeded()
        ide.expect(never()).openCommitDialog()

        limbo.onSettings(settings.copy(enabled = true))
        limbo.isCommitAllowed() shouldEqual false
    }
}