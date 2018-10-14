package limitedwip

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.mockito.Mockito
import org.mockito.verification.VerificationMode

infix fun Any.shouldEqual(expected: Any) = assertThat(this, IsEqual.equalTo(expected))

infix fun <T> T.verify(mode: VerificationMode) = Mockito.verify(this, mode)

