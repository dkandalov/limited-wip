package limitedwip.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import limitedwip.common.vcs.AllowCommitListener
import limitedwip.common.vcs.registerBeforeCommitListener
import limitedwip.shouldEqual
import org.junit.Test

class AllowCommitListenerTests : LightPlatformCodeInsightFixtureTestCase() {


    @Test fun `test that registering commit listener still via reflection still works`() {
        val wasRegistered = registerBeforeCommitListener(object: AllowCommitListener {
            override fun allowCommit(project: Project, changes: List<Change>) = true
        })
        wasRegistered shouldEqual true
    }
}