package limitedwip.common

fun <T, R> T?.ifNotNull(f: (T) -> R): R? =
    if (this != null) f(this) else null
