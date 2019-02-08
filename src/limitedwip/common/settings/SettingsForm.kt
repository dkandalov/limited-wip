package limitedwip.common.settings

import com.google.common.collect.HashBiMap
import com.intellij.ide.BrowserUtil
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.labels.LinkLabel
import limitedwip.common.settings.TcrAction.*
import java.awt.event.ActionEvent
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel

class SettingsForm(private val initialState: LimitedWipSettings) {
    lateinit var root: JPanel

    private lateinit var watchdogPanel: JPanel
    private lateinit var watchdogEnabled: JCheckBox
    private lateinit var maxLinesInChange: JComboBox<*>
    private lateinit var notificationInterval: JComboBox<*>
    private lateinit var showRemainingInToolbar: JCheckBox
    private lateinit var noCommitsAboveThreshold: JCheckBox

    private lateinit var autoRevertPanel: JPanel
    private lateinit var autoRevertEnabled: JCheckBox
    private lateinit var minutesTillRevert: JComboBox<*>
    private lateinit var notifyOnRevert: JCheckBox
    private lateinit var showTimerInToolbar: JCheckBox
    private lateinit var openReadme: LinkLabel<Void>

    private lateinit var tcrPanel: JPanel
    private lateinit var tcrEnabled: JCheckBox
    private lateinit var notifyOnTcrRevert: JCheckBox
    private lateinit var tcrActionOnPassedTest: JComboBox<*>

    private val currentState = LimitedWipSettings()
    private var isUpdatingUI: Boolean = false

    private val tcrActionByIndex = HashBiMap.create<Int, TcrAction>().also {
        it[0] = OpenCommitDialog
        it[1] = Commit
        it[2] = CommitAndPush
    }

    init {
        watchdogPanel.border = IdeBorderFactory.createTitledBorder("Change size watchdog")
        autoRevertPanel.border = IdeBorderFactory.createTitledBorder("Auto-revert")
        tcrPanel.border = IdeBorderFactory.createTitledBorder("TCR mode (test \\&\\& commit || revert)")

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
        noCommitsAboveThreshold.addActionListener(commonActionListener)

        autoRevertEnabled.addActionListener(commonActionListener)
        minutesTillRevert.addActionListener(commonActionListener)
        notifyOnRevert.addActionListener(commonActionListener)
        showTimerInToolbar.addActionListener(commonActionListener)

        tcrEnabled.addActionListener(commonActionListener)
        notifyOnTcrRevert.addActionListener(commonActionListener)
        tcrActionOnPassedTest.addActionListener(commonActionListener)

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
        noCommitsAboveThreshold.isSelected = currentState.noCommitsAboveThreshold

        autoRevertEnabled.isSelected = currentState.autoRevertEnabled
        minutesTillRevert.selectedItem = currentState.minutesTillRevert.toString()
        notifyOnRevert.isSelected = currentState.notifyOnRevert
        showTimerInToolbar.isSelected = currentState.showTimerInToolbar

        tcrEnabled.isSelected = currentState.tcrEnabled
        notifyOnTcrRevert.isSelected = currentState.notifyOnTcrRevert
        tcrActionOnPassedTest.selectedIndex = tcrActionByIndex.inverse()[currentState.tcrActionOnPassedTest]!!

        minutesTillRevert.isEnabled = currentState.autoRevertEnabled
        notifyOnRevert.isEnabled = currentState.autoRevertEnabled
        showTimerInToolbar.isEnabled = currentState.autoRevertEnabled
        maxLinesInChange.isEnabled = currentState.watchdogEnabled
        notificationInterval.isEnabled = currentState.watchdogEnabled
        showRemainingInToolbar.isEnabled = currentState.watchdogEnabled
        noCommitsAboveThreshold.isEnabled = currentState.watchdogEnabled
        notifyOnTcrRevert.isEnabled = currentState.tcrEnabled
        tcrActionOnPassedTest.isEnabled = currentState.tcrEnabled

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
            currentState.noCommitsAboveThreshold = noCommitsAboveThreshold.isSelected

            currentState.autoRevertEnabled = autoRevertEnabled.isSelected
            minutes = Integer.valueOf(minutesTillRevert.selectedItem as String)
            if (LimitedWipSettings.minutesToRevertRange.isWithin(minutes)) {
                currentState.minutesTillRevert = minutes
            }
            currentState.notifyOnRevert = notifyOnRevert.isSelected
            currentState.showTimerInToolbar = showTimerInToolbar.isSelected

            currentState.tcrEnabled = tcrEnabled.isSelected
            currentState.notifyOnTcrRevert = notifyOnTcrRevert.isSelected
            currentState.tcrActionOnPassedTest = tcrActionByIndex[tcrActionOnPassedTest.selectedIndex]!!

        } catch (ignored: NumberFormatException) {
        }
    }

    fun applyChanges(): LimitedWipSettings {
        initialState.loadState(currentState)
        return initialState
    }

    fun resetChanges() = currentState.loadState(initialState)

    fun isModified() = currentState != initialState
}
