package limitedwip.common.settings

import com.intellij.ide.BrowserUtil
import com.intellij.ui.components.labels.LinkLabel
import java.awt.event.ActionEvent
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel

class SettingsForm(private val initialState: LimitedWipSettings) {
    var root: JPanel? = null

    private lateinit var watchdogEnabled: JCheckBox
    private lateinit var maxLinesInChange: JComboBox<*>
    private lateinit var notificationInterval: JComboBox<*>
    private lateinit var showRemainingInToolbar: JCheckBox
    private lateinit var disableCommitsAboveThreshold: JCheckBox

    private lateinit var autoRevertEnabled: JCheckBox
    private lateinit var minutesTillRevert: JComboBox<*>
    private lateinit var notifyOnRevert: JCheckBox
    private lateinit var showTimerInToolbar: JCheckBox
    private lateinit var openReadme: LinkLabel<Void>

    private lateinit var limboEnabled: JCheckBox
    private lateinit var notifyOnLimboRevert: JCheckBox
    private val currentState = LimitedWipSettings()
    private var isUpdatingUI: Boolean = false

    init {
        currentState.loadState(initialState)
        updateUIFromState()

        @Suppress("RedundantLambdaArrow") // because IJ is wrong
        val commonActionListener = { _: ActionEvent ->
            updateStateFromUI()
            updateUIFromState()
        }

        watchdogEnabled.addActionListener(commonActionListener)
        maxLinesInChange.addActionListener(commonActionListener)
        notificationInterval.addActionListener(commonActionListener)
        showRemainingInToolbar.addActionListener(commonActionListener)
        disableCommitsAboveThreshold.addActionListener(commonActionListener)

        autoRevertEnabled.addActionListener(commonActionListener)
        minutesTillRevert.addActionListener(commonActionListener)
        notifyOnRevert.addActionListener(commonActionListener)
        showTimerInToolbar.addActionListener(commonActionListener)

        limboEnabled.addActionListener(commonActionListener)
        notifyOnLimboRevert.addActionListener(commonActionListener)

        openReadme.setListener(
            { _, _ -> BrowserUtil.open("https://github.com/dkandalov/limited-wip/blob/master/README.md#limited-wip") },
            null
        )
    }

    fun updateUIFromState() {
        if (isUpdatingUI) return
        isUpdatingUI = true

        watchdogEnabled.isSelected = currentState.watchdogEnabled
        maxLinesInChange.selectedItem = currentState.maxLinesInChange.toString()
        notificationInterval.selectedItem = currentState.notificationIntervalInMinutes.toString()
        showRemainingInToolbar.isSelected = currentState.showRemainingChangesInToolbar
        disableCommitsAboveThreshold.isSelected = currentState.disableCommitsAboveThreshold

        autoRevertEnabled.isSelected = currentState.autoRevertEnabled
        minutesTillRevert.selectedItem = currentState.minutesTillRevert.toString()
        notifyOnRevert.isSelected = currentState.notifyOnRevert
        showTimerInToolbar.isSelected = currentState.showTimerInToolbar

        limboEnabled.isSelected = currentState.limboEnabled
        notifyOnLimboRevert.isSelected = currentState.notifyOnLimboRevert

        minutesTillRevert.isEnabled = currentState.autoRevertEnabled
        notifyOnRevert.isEnabled = currentState.autoRevertEnabled
        showTimerInToolbar.isEnabled = currentState.autoRevertEnabled
        maxLinesInChange.isEnabled = currentState.watchdogEnabled
        notificationInterval.isEnabled = currentState.watchdogEnabled
        showRemainingInToolbar.isEnabled = currentState.watchdogEnabled
        disableCommitsAboveThreshold.isEnabled = currentState.watchdogEnabled
        notifyOnLimboRevert.isEnabled = currentState.limboEnabled

        isUpdatingUI = false
    }

    private fun updateStateFromUI() {
        try {
            currentState.watchdogEnabled = watchdogEnabled.isSelected
            val lineCount = Integer.valueOf(maxLinesInChange.selectedItem as String)
            if (LimitedWipSettings.changedLinesRange.isWithin(lineCount)) {
                currentState.maxLinesInChange = lineCount
            }
            var minutes = Integer.valueOf(notificationInterval.selectedItem as String)!!
            if (LimitedWipSettings.notificationIntervalRange.isWithin(minutes)) {
                currentState.notificationIntervalInMinutes = minutes
            }
            currentState.showRemainingChangesInToolbar = showRemainingInToolbar.isSelected
            currentState.disableCommitsAboveThreshold = disableCommitsAboveThreshold.isSelected

            currentState.autoRevertEnabled = autoRevertEnabled.isSelected
            minutes = Integer.valueOf(minutesTillRevert.selectedItem as String)
            if (LimitedWipSettings.minutesToRevertRange.isWithin(minutes)) {
                currentState.minutesTillRevert = minutes
            }
            currentState.notifyOnRevert = notifyOnRevert.isSelected
            currentState.showTimerInToolbar = showTimerInToolbar.isSelected

            currentState.limboEnabled = limboEnabled.isSelected
            currentState.notifyOnLimboRevert = notifyOnLimboRevert.isSelected

        } catch (ignored: NumberFormatException) {
        }
    }

    fun applyChanges(): LimitedWipSettings {
        initialState.loadState(currentState)
        return initialState
    }

    fun resetChanges() {
        currentState.loadState(initialState)
    }

    fun isModified() = currentState != initialState
}
