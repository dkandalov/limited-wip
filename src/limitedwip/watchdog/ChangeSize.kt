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

data class ChangeSizesWithPath(val value: List<Pair<String, ChangeSize>>) {

    val totalChangeSize = value.map { it.second }.fold(ChangeSize.empty) { acc, it -> acc + it }

    fun add(path: String, changeSize: ChangeSize) =
        ChangeSizesWithPath(value + Pair(path, changeSize))

    companion object {
        val empty = ChangeSizesWithPath(emptyList())
    }
}