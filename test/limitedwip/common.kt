package limitedwip

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual

infix fun Any.shouldEqual(expected: Any) {
    assertThat(this, IsEqual.equalTo(expected))
}
