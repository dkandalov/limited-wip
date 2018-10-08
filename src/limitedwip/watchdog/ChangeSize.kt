package limitedwip.watchdog

data class ChangeSize(val value: Int, val isApproximate: Boolean = false) {
    fun add(that: ChangeSize) = ChangeSize(value + that.value, isApproximate or that.isApproximate)
}
