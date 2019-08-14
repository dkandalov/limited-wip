package limitedwip.tcr.components

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestsUIUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsAdapter
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicReference

class UnitTestsWatcher(private val project: Project) {

    fun start(listener: Listener) {
        val profileNameRef = AtomicReference<String>()

        project.messageBus.connect(project).subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
            override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int) {
                profileNameRef.set(env.runProfile.name)
            }
        })

        project.messageBus.connect(project).subscribe(Notifications.TOPIC, object : NotificationsAdapter() {
            override fun notify(notification: Notification) {
                if (notification.groupId == TestsUIUtil.NOTIFICATION_GROUP.displayId) {
                    // Can happen when test run configuration has wrong test class name.
                    // Console output might be something like this: Class not found: "com.MyTest"
                    if (notification.content.contains(" 0 passed")) return

                    val testsFailed = notification.type == NotificationType.ERROR
                    val profileName = profileNameRef.get()

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
