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
package limitedwip.autorevert


import limitedwip.autorevert.components.IdeAdapter

class AutoRevert(private val ideNotifications: IdeAdapter) {

    private var settings: Settings? = null
    var isStarted = false
        private set
    private var startSeconds: Int = 0
    private var remainingSeconds: Int = 0

    fun init(settings: Settings): AutoRevert {
        onSettings(settings)
        return this
    }

    fun start() {
        if (!settings!!.autoRevertEnabled) return

        isStarted = true
        startSeconds = -1
        applyNewSettings()

        ideNotifications.onAutoRevertStarted(remainingSeconds)
    }

    fun stop() {
        isStarted = false
        ideNotifications.onAutoRevertStopped()
    }

    fun onTimer(seconds: Int) {
        if (!isStarted) return

        if (startSeconds == -1) {
            startSeconds = seconds - 1
        }
        val secondsPassed = seconds - startSeconds

        ideNotifications.onTimeTillRevert(remainingSeconds - secondsPassed + 1)

        if (secondsPassed >= remainingSeconds) {
            startSeconds = -1
            applyNewSettings()
            val revertedFilesCount = ideNotifications.revertCurrentChangeList()
            if (revertedFilesCount > 0 && settings!!.notifyOnRevert) {
                ideNotifications.onChangesRevert()
            }
        }
    }

    fun onAllFilesCommitted() {
        if (!isStarted) return

        startSeconds = -1
        applyNewSettings()
        ideNotifications.onCommit(remainingSeconds)
    }

    fun onSettings(settings: Settings) {
        ideNotifications.onSettingsUpdate(settings)
        this.settings = settings
        if (isStarted && !settings.autoRevertEnabled) {
            stop()
        }
    }

    private fun applyNewSettings() {
        if (remainingSeconds != settings!!.secondsTillRevert) {
            remainingSeconds = settings!!.secondsTillRevert
        }
    }


    data class Settings(
        val autoRevertEnabled: Boolean,
        val secondsTillRevert: Int,
        val notifyOnRevert: Boolean,
        val showTimerInToolbar: Boolean = true
    ) {
        constructor(secondsTillRevert: Int) : this(true, secondsTillRevert, true)
    }
}
