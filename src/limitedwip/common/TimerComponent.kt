/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package limitedwip.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit


class TimerComponent : ApplicationComponent {

    private val timer = Timer(PluginId.value + "-TimeEvents")
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val startTime = System.currentTimeMillis()


    override fun initComponent() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                try {
                    val secondsSinceStart = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)
                    for (listener in listeners) {
                        listener.onUpdate(secondsSinceStart.toInt())
                    }
                } catch (ignored: ProcessCanceledException) {
                } catch (e: Exception) {
                    log.error(e)
                }

            }
        }, 0, oneSecondMs)
    }

    override fun disposeComponent() {
        timer.cancel()
        timer.purge()
    }

    override fun getComponentName(): String {
        return PluginId.value + "-" + TimerComponent::class.java.simpleName
    }

    fun addListener(listener: Listener, parentDisposable: Disposable) {
        listeners.add(listener)
        Disposer.register(parentDisposable, Disposable { listeners.remove(listener) })
    }

    interface Listener {
        fun onUpdate(seconds: Int)
    }

    companion object {
        private val log = Logger.getInstance(TimerComponent::class.java)
        private val oneSecondMs: Long = 1000
    }
}
