package limitedwip.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit.MILLISECONDS

class TimerAppComponent {
    private val log = Logger.getInstance(TimerAppComponent::class.java)
    private val timer = Timer("$pluginId-TimeEvents")
    private val listeners = CopyOnWriteArrayList<Listener>()

    init {
        val startTime = System.currentTimeMillis()
        var lastSecondsSinceStart = 0L
        val task = object: TimerTask() {
            override fun run() {
                try {
                    val secondsSinceStart = MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)
                    if (secondsSinceStart != lastSecondsSinceStart) {
                        lastSecondsSinceStart = secondsSinceStart
                        for (listener in listeners) {
                            listener.onUpdate()
                        }
                    }
                } catch (ignored: ProcessCanceledException) {
                } catch (e: Exception) {
                    log.error(e)
                }
            }
        }
        val periodInMillis = 500L
        timer.schedule(task, 0, periodInMillis)
    }

    fun addListener(parentDisposable: Disposable, listener: Listener) {
        listeners.add(listener)
        Disposer.register(parentDisposable, Disposable { listeners.remove(listener) })
    }

    interface Listener {
        fun onUpdate()
    }

    companion object {
        fun getInstance(): TimerAppComponent =
            ApplicationManager.getApplication().getComponent(TimerAppComponent::class.java)
    }
}
