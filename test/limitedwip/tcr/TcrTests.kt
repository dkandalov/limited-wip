package limitedwip.tcr

import limitedwip.common.settings.TcrAction.OpenCommitDialog
import limitedwip.expect
import limitedwip.shouldEqual
import limitedwip.tcr.Tcr.ChangeListModifications
import limitedwip.tcr.components.Ide
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never

class TcrTests {
    private val ide = mock(Ide::class.java)
    private val settings = Tcr.Settings(
        enabled = true,
        notifyOnRevert = true,
        openCommitDialogOnPassedTest = true,
        actionOnPassedTest = OpenCommitDialog
    )
    private val tcr = Tcr(ide, settings)
    private val someModifications = ChangeListModifications(mapOf("foo" to 1L))

    @Test fun `allow commits only after running a unit test`() {
        tcr.isCommitAllowed(someModifications) shouldEqual false
        ide.expect().notifyThatCommitWasCancelled()

        tcr.onUnitTestSucceeded(someModifications)
        tcr.isCommitAllowed(someModifications) shouldEqual true
    }

    @Test fun `show commit dialog after successful test run`() {
        tcr.onUnitTestSucceeded(someModifications)
        ide.expect().openCommitDialog()
    }

    @Test fun `don't show commit dialog if there are no modifications`() {
        val noModifications = ChangeListModifications(emptyMap())
        tcr.onUnitTestSucceeded(noModifications)
        ide.expect(never()).openCommitDialog()
    }

    @Test fun `don't show commit dialog if it's disabled in settings`() {
        tcr.onSettingsUpdate(settings.copy(openCommitDialogOnPassedTest = false))
        tcr.onUnitTestSucceeded(someModifications)
        ide.expect(never()).openCommitDialog()
    }

    @Test fun `after commit need to run a unit test to be able to commit again`() {
        tcr.onUnitTestSucceeded(someModifications)
        tcr.isCommitAllowed(someModifications) shouldEqual true

        tcr.onSuccessfulCommit()
        tcr.isCommitAllowed(someModifications) shouldEqual false

        tcr.onUnitTestSucceeded(someModifications)
        tcr.isCommitAllowed(someModifications) shouldEqual true
    }

    @Test fun `don't allow commits if files were changed after running a unit test`() {
        tcr.onUnitTestSucceeded(someModifications)
        tcr.isCommitAllowed(someModifications) shouldEqual true

        val moreModifications = ChangeListModifications(someModifications.value + Pair("foo", 2L))
        tcr.isCommitAllowed(moreModifications) shouldEqual false

        tcr.onUnitTestSucceeded(moreModifications)
        tcr.isCommitAllowed(moreModifications) shouldEqual true
    }

    @Test fun `revert changes on failed unit test`() {
        tcr.onUnitTestFailed()
        ide.expect().revertCurrentChangeList()
    }

    @Test fun `notify user on revert`() {
        `revert changes on failed unit test`()
        ide.expect().notifyThatChangesWereReverted()
    }

    @Test fun `don't notify user on revert if notification is disabled in settings`() {
        tcr.onSettingsUpdate(settings.copy(notifyOnRevert = false))
        `revert changes on failed unit test`()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `can do one-off commit without running a unit test`() {
        tcr.isCommitAllowed(someModifications) shouldEqual false
        tcr.forceOneCommit()
        tcr.isCommitAllowed(someModifications) shouldEqual true
        ide.expect().openCommitDialog()

        tcr.onSuccessfulCommit()
        tcr.isCommitAllowed(someModifications) shouldEqual false
    }

    @Test fun `if disabled, always allow commits`() {
        tcr.onSettingsUpdate(settings.copy(enabled = false))
        tcr.isCommitAllowed(someModifications) shouldEqual true
    }

    @Test fun `if disabled, don't revert changes on failed unit test`() {
        tcr.onSettingsUpdate(settings.copy(enabled = false))
        tcr.onUnitTestFailed()
        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `if disabled, don't show commit dialog and don't count successful test runs`() {
        tcr.onSettingsUpdate(settings.copy(enabled = false))
        tcr.onUnitTestSucceeded(someModifications)
        ide.expect(never()).openCommitDialog()

        tcr.onSettingsUpdate(settings.copy(enabled = true))
        tcr.isCommitAllowed(someModifications) shouldEqual false
    }
}