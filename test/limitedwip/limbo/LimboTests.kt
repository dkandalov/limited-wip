package limitedwip.limbo

import limitedwip.expect
import limitedwip.limbo.Limbo.ChangeListModifications
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
    private val someModifications = ChangeListModifications(mapOf("foo" to 1L))

    @Test fun `allow commits only after running a unit test`() {
        limbo.isCommitAllowed(someModifications) shouldEqual false
        ide.expect().notifyThatCommitWasCancelled()

        limbo.onUnitTestSucceeded(someModifications)
        limbo.isCommitAllowed(someModifications) shouldEqual true
    }

    @Test fun `show commit dialog after successful test run`() {
        limbo.onUnitTestSucceeded(someModifications)
        ide.expect().openCommitDialog()
    }

    @Test fun `don't show commit dialog is it's disabled in settings`() {
        limbo.onSettings(settings.copy(openCommitDialogOnPassedTest = false))
        limbo.onUnitTestSucceeded(someModifications)
        ide.expect(never()).openCommitDialog()
    }

    @Test fun `after commit need to run a unit test to be able to commit again`() {
        limbo.onUnitTestSucceeded(someModifications)
        limbo.isCommitAllowed(someModifications) shouldEqual true

        limbo.onSuccessfulCommit()
        limbo.isCommitAllowed(someModifications) shouldEqual false

        limbo.onUnitTestSucceeded(someModifications)
        limbo.isCommitAllowed(someModifications) shouldEqual true
    }

    @Test fun `don't allow commits if files were changed after running a unit test`() {
        limbo.onUnitTestSucceeded(someModifications)
        limbo.isCommitAllowed(someModifications) shouldEqual true

        val moreModifications = ChangeListModifications(someModifications.value + Pair("foo", 2L))
        limbo.isCommitAllowed(moreModifications) shouldEqual false

        limbo.onUnitTestSucceeded(moreModifications)
        limbo.isCommitAllowed(moreModifications) shouldEqual true
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
        limbo.isCommitAllowed(someModifications) shouldEqual false
        limbo.forceOneCommit()
        limbo.isCommitAllowed(someModifications) shouldEqual true
        ide.expect().openCommitDialog()

        limbo.onSuccessfulCommit()
        limbo.isCommitAllowed(someModifications) shouldEqual false
    }

    @Test fun `if disabled, always allow commits`() {
        limbo.onSettings(settings.copy(enabled = false))
        limbo.isCommitAllowed(someModifications) shouldEqual true
    }

    @Test fun `if disabled, don't revert changes on failed unit test`() {
        limbo.onSettings(settings.copy(enabled = false))
        limbo.onUnitTestFailed()
        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `if disabled, don't show commit dialog and don't count successful test runs`() {
        limbo.onSettings(settings.copy(enabled = false))
        limbo.onUnitTestSucceeded(someModifications)
        ide.expect(never()).openCommitDialog()

        limbo.onSettings(settings.copy(enabled = true))
        limbo.isCommitAllowed(someModifications) shouldEqual false
    }
}