package limitedwip.tcr.components

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager.Companion.EXECUTION_TOPIC
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestsUIUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType.ERROR
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicReference

class UnitTestsWatcher(private val project: Project) {

    fun start(listener: Listener) {
        val profileNameRef = AtomicReference<String?>()

        project.messageBus.connect(project).subscribe(EXECUTION_TOPIC, object : ExecutionListener {
            override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int) {
                profileNameRef.set(env.runProfile.name)
            }
        })

        project.messageBus.connect(project).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                if (notification.groupId == TestsUIUtil.NOTIFICATION_GROUP.displayId) {
                    // We don't consider it success or failure when:
                    //  - run configuration had wrong test class name (e.g. the test class was manually renamed since the last test run)
                    //  - the test was terminated by user in IDE (e.g. the test was running for too long)
                    // A hacky way to determine it is by checking console output which might contains one of the string below.
                    if (notification.content.contains(" 0 passed") || notification.content.contains("Tests passed: 0")) return

                    val testsFailed = notification.type == ERROR
                    // It seems to be possible to get profileName as null probably because
                    // Notifications.TOPIC callback is invoked before ExecutionManager.EXECUTION_TOPIC.
                    // Using below some default value just to avoid null.
                    val profileName = profileNameRef.get() ?: "unknown-profile"

                    if (testsFailed) listener.onUnitTestFailed(profileName)
                    else listener.onUnitTestSucceeded(profileName)
                }
            }
        })
    }

    interface Listener {
        fun onUnitTestSucceeded(testName: String)
        fun onUnitTestFailed(testName: String)
    }
}
