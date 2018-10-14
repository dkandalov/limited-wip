package limitedwip.limbo.components

import com.intellij.execution.testframework.TestsUIUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsAdapter
import com.intellij.openapi.project.Project

class UnitTestsWatcher(project: Project) {
    private val busConnection = project.messageBus.connect(project)

    fun start(listener: Listener) {
        busConnection.subscribe(Notifications.TOPIC, object : NotificationsAdapter() {
            override fun notify(notification: Notification) {
                if (notification.groupId == TestsUIUtil.NOTIFICATION_GROUP.displayId) {
                    val testsFailed = notification.type == NotificationType.ERROR
                    if (testsFailed) listener.onUnitTestFailed()
                    else listener.onUnitTestSucceeded()
                }
            }
        })
    }

    interface Listener {
        fun onUnitTestSucceeded()
        fun onUnitTestFailed()
    }
}
