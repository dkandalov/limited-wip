package limitedwip.watchdog

data class ChangeSize(
    val value: Int,
    val isApproximate: Boolean = false
) {
    operator fun plus(that: ChangeSize) = ChangeSize(value + that.value, isApproximate or that.isApproximate)

    companion object {
        val NA = ChangeSize(-1)
        val empty = ChangeSize(0)
        val approximatelyEmpty = ChangeSize(0, isApproximate = true)
    }
}
