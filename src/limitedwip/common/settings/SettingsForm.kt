package limitedwip.common.settings

import com.google.common.collect.HashBiMap
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.util.Ref
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.execution.ParametersListUtil.COLON_LINE_JOINER
import com.intellij.util.execution.ParametersListUtil.COLON_LINE_PARSER
import limitedwip.common.settings.CommitMessageSource.ChangeListName
import limitedwip.common.settings.CommitMessageSource.LastCommit
import limitedwip.common.settings.CommitMessageSource.AIAssistant
import limitedwip.common.settings.LimitedWipSettings.Companion.isValidChangedSizeRange
import limitedwip.common.settings.LimitedWipSettings.Companion.isValidNotificationInterval
import limitedwip.common.settings.LimitedWipSettings.Companion.isValidTimeTillRevert
import limitedwip.common.settings.LimitedWipSettings.Companion.never
import limitedwip.common.settings.TcrAction.*
import limitedwip.common.settings.TimeUnit.Minutes
import limitedwip.common.settings.TimeUnit.Seconds
import java.awt.event.ActionEvent
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class SettingsForm(private val initialState: LimitedWipSettings) {
    lateinit var root: JPanel

    private lateinit var watchdogPanel: JPanel
    private lateinit var watchdogEnabled: JCheckBox
    private lateinit var maxLinesInChange: JComboBox<*>
    private lateinit var notificationInterval: JComboBox<*>
    private lateinit var showRemainingInToolbar: JCheckBox
    private lateinit var noCommitsAboveThreshold: JCheckBox
    private lateinit var exclusions: RawCommandLineEditor
    private lateinit var notificationMinutesLabel: JLabel

    private lateinit var autoRevertPanel: JPanel
    private lateinit var autoRevertEnabled: JCheckBox
    private lateinit var timeTillRevert: JComboBox<*>
    private lateinit var timeUnitTillRevert: JComboBox<*>
    private lateinit var notifyOnRevert: JCheckBox
    private lateinit var showTimerInToolbar: JCheckBox
    private lateinit var openReadme: LinkLabel<Void>

    private lateinit var tcrPanel: JPanel
    private lateinit var tcrEnabled: JCheckBox
    private lateinit var notifyOnTcrRevert: JCheckBox
    private lateinit var tcrActionOnPassedTest: JComboBox<*>
    private lateinit var commitMessageSource: JComboBox<*>
    private lateinit var doNotRevertTests: JCheckBox
    private lateinit var doNotRevertFiles: RawCommandLineEditor

    private val currentState = LimitedWipSettings()
    private var isUpdating = Ref(false)

    private val tcrActionByIndex = HashBiMap.create<Int, TcrAction>().also {
        it[0] = Commit
        it[1] = AmendCommit
        it[2] = CommitAndPush
        it[3] = OpenCommitDialog
    }

    private val commitMessageSourceByIndex = HashBiMap.create<Int, CommitMessageSource>().also {
        it[0] = LastCommit
        it[1] = ChangeListName
        it[2] = AIAssistant
    }

    private val timeUnitByIndex = HashBiMap.create<Int, TimeUnit>().also {
        it[0] = Seconds
        it[1] = Minutes
    }

    private fun createUIComponents() {
        exclusions = RawCommandLineEditor(COLON_LINE_PARSER, COLON_LINE_JOINER)
        doNotRevertFiles = RawCommandLineEditor(COLON_LINE_PARSER, COLON_LINE_JOINER)
    }

    init {
        watchdogPanel.border = IdeBorderFactory.createTitledBorder("Change Size Watchdog")
        autoRevertPanel.border = IdeBorderFactory.createTitledBorder("Auto-Revert")
        tcrPanel.border = IdeBorderFactory.createTitledBorder("TCR (Test \\&\\& Commit || Revert)")

        currentState.loadState(initialState)
        updateUIFromState()

        val commonActionListener = { _: ActionEvent -> fullUpdate() }
        val commonDocumentListener = object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = noReentryWhen(isUpdating) {
                updateStateFromUI()
            }
        }

        watchdogEnabled.addActionListener(commonActionListener)
        maxLinesInChange.addActionListener(commonActionListener)
        notificationInterval.addActionListener(commonActionListener)
        showRemainingInToolbar.addActionListener(commonActionListener)
        noCommitsAboveThreshold.addActionListener(commonActionListener)
        exclusions.textField.document.addDocumentListener(commonDocumentListener)
        // It would be great to have exclusions label with html like in com.intellij.compiler.options.CompilerUIConfigurable.CompilerUIConfigurable
        // but I failed to make it properly wrap its text without expanding horizontally to full width of the settings window.

        autoRevertEnabled.addActionListener(commonActionListener)
        timeTillRevert.addActionListener(commonActionListener)
        timeUnitTillRevert.addActionListener(commonActionListener)
        notifyOnRevert.addActionListener(commonActionListener)
        showTimerInToolbar.addActionListener(commonActionListener)

        tcrEnabled.addActionListener(commonActionListener)
        notifyOnTcrRevert.addActionListener(commonActionListener)
        tcrActionOnPassedTest.addActionListener(commonActionListener)
        commitMessageSource.addActionListener(commonActionListener)
        doNotRevertTests.addActionListener(commonActionListener)
        doNotRevertFiles.textField.document.addDocumentListener(commonDocumentListener)

        openReadme.setListener(
            { _, _ -> BrowserUtil.open("https://github.com/dkandalov/limited-wip/blob/master/README.md#limited-wip") },
            null
        )
    }

    private fun fullUpdate() = noReentryWhen(isUpdating) {
        updateStateFromUI()
        updateUIFromState()
    }

    private fun updateUIFromState() {
        watchdogEnabled.isSelected = currentState.watchdogEnabled
        maxLinesInChange.selectedItem = currentState.maxLinesInChange.toString()
        notificationInterval.selectedItem = currentState.notificationIntervalInMinutes.toIntervalString()
        notificationMinutesLabel.isVisible = currentState.notificationIntervalInMinutes != never
        showRemainingInToolbar.isSelected = currentState.showRemainingChangesInToolbar
        noCommitsAboveThreshold.isSelected = currentState.noCommitsAboveThreshold
        exclusions.text = currentState.exclusions

        autoRevertEnabled.isSelected = currentState.autoRevertEnabled
        timeTillRevert.selectedItem = currentState.timeTillRevert.toString()
        timeUnitTillRevert.selectedIndex = timeUnitByIndex.inverse()[currentState.timeUnitTillRevert]!!
        notifyOnRevert.isSelected = currentState.notifyOnRevert
        showTimerInToolbar.isSelected = currentState.showTimerInToolbar

        tcrEnabled.isSelected = currentState.tcrEnabled
        notifyOnTcrRevert.isSelected = currentState.notifyOnTcrRevert
        tcrActionOnPassedTest.selectedIndex = tcrActionByIndex.inverse()[currentState.tcrActionOnPassedTest]!!
        commitMessageSource.selectedIndex = commitMessageSourceByIndex.inverse()[currentState.commitMessageSource]!!
        doNotRevertTests.isSelected = currentState.doNotRevertTests
        doNotRevertFiles.text = currentState.doNotRevertFiles

        currentState.autoRevertEnabled.let {
            timeTillRevert.isEnabled = it
            timeUnitTillRevert.isEnabled = it
            notifyOnRevert.isEnabled = it
            showTimerInToolbar.isEnabled = it
        }
        currentState.watchdogEnabled.let {
            maxLinesInChange.isEnabled = it
            notificationInterval.isEnabled = it
            showRemainingInToolbar.isEnabled = it
            noCommitsAboveThreshold.isEnabled = it
            exclusions.isEnabled = it
        }
        currentState.tcrEnabled.let {
            notifyOnTcrRevert.isEnabled = it
            tcrActionOnPassedTest.isEnabled = it
            commitMessageSource.isEnabled = it && currentState.tcrActionOnPassedTest != OpenCommitDialog
            doNotRevertTests.isEnabled = it
            doNotRevertFiles.isEnabled = it
        }
    }

    private fun updateStateFromUI() {
        try {
            currentState.watchdogEnabled = watchdogEnabled.isSelected
            val lineCount = (maxLinesInChange.selectedItem as String).toInt()
            if (isValidChangedSizeRange(lineCount)) {
                currentState.maxLinesInChange = lineCount
            }
            val notificationInMinutes = (notificationInterval.selectedItem as String).parseInterval()
            if (isValidNotificationInterval(notificationInMinutes)) {
                currentState.notificationIntervalInMinutes = notificationInMinutes
            }
            currentState.showRemainingChangesInToolbar = showRemainingInToolbar.isSelected
            currentState.noCommitsAboveThreshold = noCommitsAboveThreshold.isSelected

            currentState.exclusions = exclusions.text

            currentState.autoRevertEnabled = autoRevertEnabled.isSelected
            val timeTillRevert = (timeTillRevert.selectedItem as String).toInt()
            if (isValidTimeTillRevert(timeTillRevert)) {
                currentState.timeTillRevert = timeTillRevert
            }
            currentState.timeUnitTillRevert = timeUnitByIndex[timeUnitTillRevert.selectedIndex]!!
            currentState.notifyOnRevert = notifyOnRevert.isSelected
            currentState.showTimerInToolbar = showTimerInToolbar.isSelected

            currentState.tcrEnabled = tcrEnabled.isSelected
            currentState.notifyOnTcrRevert = notifyOnTcrRevert.isSelected
            currentState.tcrActionOnPassedTest = tcrActionByIndex[tcrActionOnPassedTest.selectedIndex]!!
            currentState.commitMessageSource = commitMessageSourceByIndex[commitMessageSource.selectedIndex]!!
            currentState.doNotRevertTests = doNotRevertTests.isSelected
            currentState.doNotRevertFiles = doNotRevertFiles.text
        } catch (ignored: NumberFormatException) {
        }
    }

    fun applyChanges(): LimitedWipSettings {
        initialState.loadState(currentState)
        return initialState
    }

    fun resetChanges() {
        currentState.loadState(initialState)
        noReentryWhen(isUpdating) {
            updateUIFromState()
        }
    }

    fun isModified() = currentState != initialState

    private inline fun noReentryWhen(ref: Ref<Boolean>, f: () -> Unit) {
        if (!ref.get()) {
            ref.set(true)
            f()
            ref.set(false)
        }
    }
}

private fun String.parseInterval() = if (this == "never") never else toInt()

private fun Int.toIntervalString() = if (this == never) "never" else toString()
