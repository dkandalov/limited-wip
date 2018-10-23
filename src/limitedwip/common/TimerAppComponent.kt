// Because ApplicationComponent was deprecated relatively recently.
@file:Suppress("DEPRECATION")

package limitedwip.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class TimerAppComponent : ApplicationComponent {
    private val log = Logger.getInstance(TimerAppComponent::class.java)
    private val timer = Timer("$pluginId-TimeEvents")
    private val listeners = CopyOnWriteArrayList<Listener>()

    override fun initComponent() {
        val startTime = System.currentTimeMillis()
        val task = object: TimerTask() {
            override fun run() {
                try {
                    val secondsSinceStart = MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)
                    for (listener in listeners) {
                        listener.onUpdate(secondsSinceStart.toInt())
                    }
                } catch (ignored: ProcessCanceledException) {
                } catch (e: Exception) {
                    log.error(e)
                }
            }
        }
        timer.schedule(task, 0, SECONDS.toMillis(1))
    }

    override fun disposeComponent() {
        timer.cancel()
        timer.purge()
    }

    override fun getComponentName(): String = "$pluginId-${this.javaClass.simpleName}"

    fun addListener(listener: Listener, parentDisposable: Disposable) {
        listeners.add(listener)
        Disposer.register(parentDisposable, Disposable { listeners.remove(listener) })
    }

    interface Listener {
        fun onUpdate(seconds: Int)
    }

    companion object {
        fun getInstance(): TimerAppComponent =
            ApplicationManager.getApplication().getComponent(TimerAppComponent::class.java)
    }
}
