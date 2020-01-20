package limitedwip.common

import limitedwip.common.settings.LimitedWipSettings
import limitedwip.common.settings.LimitedWipSettings.Companion.never
import limitedwip.common.settings.TimeUnit.Minutes
import limitedwip.shouldEqual
import org.junit.Test

@Suppress("DEPRECATION")
class SettingsMigrationTests {
    @Test fun `when loading old settings data is moved into new fields`() {
        val oldSettings = LimitedWipSettings(minutesTillRevert = 5)
        val settings = LimitedWipSettings()
        settings.loadState(oldSettings)

        settings.minutesTillRevert shouldEqual never
        settings.timeTillRevert shouldEqual 5
        settings.timeUnitTillRevert shouldEqual Minutes
    }
}