package limitedwip.common.settings

import com.google.common.collect.HashBiMap
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.util.Ref
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.execution.ParametersListUtil
import limitedwip.common.settings.CommitMessageSource.*
import limitedwip.common.settings.LimitedWipSettings.Companion.isValidChangedSizeRange
import limitedwip.common.settings.LimitedWipSettings.Companion.isValidMinutesTillRevert
import limitedwip.common.settings.LimitedWipSettings.Companion.isValidNotificationInterval
import limitedwip.common.settings.TcrAction.*
import java.awt.event.ActionEvent
import javax.swing.JCheckBox
import javax.swing.JComboBox
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
    private lateinit var commitMessageSource: JComboBox<*>
    private lateinit var doNotRevertTests: JCheckBox
    private lateinit var doNotRevertFiles: RawCommandLineEditor

    private val currentState = LimitedWipSettings()
    private var isUpdating = Ref(false)

    private val tcrActionByIndex = HashBiMap.create<Int, TcrAction>().also {
        it[0] = Commit
        it[1] = CommitAndPush
        it[2] = OpenCommitDialog
    }

    private val commitMessageSourceByIndex = HashBiMap.create<Int, CommitMessageSource>().also {
        it[0] = LastCommit
        it[1] = ChangeListName
    }

    private fun createUIComponents() {
        exclusions = RawCommandLineEditor(ParametersListUtil.COLON_LINE_PARSER, ParametersListUtil.COLON_LINE_JOINER)
        exclusions.dialogCaption = "Resource patterns"

        doNotRevertFiles = RawCommandLineEditor(ParametersListUtil.COLON_LINE_PARSER, ParametersListUtil.COLON_LINE_JOINER)
        doNotRevertFiles.dialogCaption = "Resource patterns"
    }

    init {
        watchdogPanel.border = IdeBorderFactory.createTitledBorder("Change size watchdog")
        autoRevertPanel.border = IdeBorderFactory.createTitledBorder("Auto-revert")
        tcrPanel.border = IdeBorderFactory.createTitledBorder("TCR mode (test \\&\\& commit || revert)")

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
        minutesTillRevert.addActionListener(commonActionListener)
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
        notificationInterval.selectedItem = currentState.notificationIntervalInMinutes.toString()
        showRemainingInToolbar.isSelected = currentState.showRemainingChangesInToolbar
        noCommitsAboveThreshold.isSelected = currentState.noCommitsAboveThreshold
        exclusions.text = currentState.exclusions

        autoRevertEnabled.isSelected = currentState.autoRevertEnabled
        minutesTillRevert.selectedItem = currentState.minutesTillRevert.toString()
        notifyOnRevert.isSelected = currentState.notifyOnRevert
        showTimerInToolbar.isSelected = currentState.showTimerInToolbar

        tcrEnabled.isSelected = currentState.tcrEnabled
        notifyOnTcrRevert.isSelected = currentState.notifyOnTcrRevert
        tcrActionOnPassedTest.selectedIndex = tcrActionByIndex.inverse()[currentState.tcrActionOnPassedTest]!!
        commitMessageSource.selectedIndex = commitMessageSourceByIndex.inverse()[currentState.commitMessageSource]!!
        doNotRevertTests.isSelected = currentState.doNotRevertTests
        doNotRevertFiles.text = currentState.doNotRevertFiles

        currentState.autoRevertEnabled.let {
            minutesTillRevert.isEnabled = it
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
            commitMessageSource.isEnabled = it
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
            var minutes = (notificationInterval.selectedItem as String).toInt()
            if (isValidNotificationInterval(minutes)) {
                currentState.notificationIntervalInMinutes = minutes
            }
            currentState.showRemainingChangesInToolbar = showRemainingInToolbar.isSelected
            currentState.noCommitsAboveThreshold = noCommitsAboveThreshold.isSelected

            currentState.exclusions = exclusions.text

            currentState.autoRevertEnabled = autoRevertEnabled.isSelected
            minutes = (minutesTillRevert.selectedItem as String).toInt()
            if (isValidMinutesTillRevert(minutes)) {
                currentState.minutesTillRevert = minutes
            }
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
