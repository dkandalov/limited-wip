package limitedwip

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.mockito.InOrder
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.verification.VerificationMode

infix fun Any.shouldEqual(expected: Any) = assertThat(this, IsEqual.equalTo(expected))

fun <T> T.expect(mode: VerificationMode = times(1)): T = Mockito.verify(this, mode)
fun <T> T.expect(inOrder: InOrder, mode: VerificationMode = times(1)): T = inOrder.verify(this, mode)

fun Any.expectNoMoreInteractions() = Mockito.verifyNoMoreInteractions(this)

