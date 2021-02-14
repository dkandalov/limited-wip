package limitedwip.common

import limitedwip.common.settings.LimitedWipSettings.Companion.never
import limitedwip.common.settings.OldLocation_LimitedWipSettings
import limitedwip.common.settings.TimeUnit.Minutes
import limitedwip.shouldEqual
import org.junit.Test

@Suppress("DEPRECATION")
class OldLocation_SettingsMigrationTests {
    @Test fun `when loading old settings data is moved into new fields`() {
        val oldSettings = OldLocation_LimitedWipSettings(minutesTillRevert = 5)
        val settings = OldLocation_LimitedWipSettings()
        settings.loadState(oldSettings)

        settings.minutesTillRevert shouldEqual never
        settings.timeTillRevert shouldEqual 5
        settings.timeUnitTillRevert shouldEqual Minutes
    }
}