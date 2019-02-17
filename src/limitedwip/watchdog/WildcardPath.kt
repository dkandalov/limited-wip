package limitedwip.watchdog

import com.intellij.compiler.CompilerConfigurationImpl.isPatternNegated
import com.intellij.compiler.MalformedPatternException
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.utils.inlays.InlayHintsChecker.Companion.pattern
import org.apache.oro.text.regex.Perl5Compiler
import org.jetbrains.annotations.NonNls

data class CompiledPattern(
    val fileNameRegex: Regex,
    val dirRegex: Regex?
) {
    fun matches(filePath: String): Boolean {
        val slash = filePath.lastIndexOf('/')
        return if (slash >= 0) {
            val dir = filePath.substring(0, slash + 1)
            val fileName = filePath.substring(slash + 1)
            fileNameRegex.matches(fileName) && (dirRegex == null || dirRegex.matches(dir))
        } else {
            fileNameRegex.matches(filePath)
        }
    }
}

fun convertToRegexp(wildcardPattern: String): CompiledPattern {
    var pattern = wildcardPattern
    if (isPatternNegated(pattern)) {
        pattern = pattern.substring(1)
    }
    pattern = FileUtil.toSystemIndependentName(pattern)

    var dirPattern: String? = null
    val slash = pattern.lastIndexOf('/')
    if (slash >= 0) {
        dirPattern = pattern.substring(0, slash + 1)
        pattern = pattern.substring(slash + 1)
        dirPattern = optimizeDirPattern(dirPattern)
    }

    pattern = normalizeWildcards(pattern)
    pattern = optimize(pattern)

    val dirCompiled = if (dirPattern == null) null else compilePattern(dirPattern)
    return CompiledPattern(Regex(compilePattern(pattern)), if (dirCompiled == null) null else Regex(dirCompiled))
}

@Throws(MalformedPatternException::class)
private fun compilePattern(@NonNls s: String): String {
    try {
        val compiler = Perl5Compiler()
        return if (SystemInfo.isFileSystemCaseSensitive) compiler.compile(s).pattern
        else compiler.compile(s, Perl5Compiler.CASE_INSENSITIVE_MASK).pattern
    } catch (ex: org.apache.oro.text.regex.MalformedPatternException) {
        throw MalformedPatternException(ex)
    }
}

private fun normalizeWildcards(wildcardPattern: String): String {
    var pattern = wildcardPattern
    pattern = StringUtil.replace(pattern, "\\!", "!")
    pattern = StringUtil.replace(pattern, ".", "\\.")
    pattern = StringUtil.replace(pattern, "*?", ".+")
    pattern = StringUtil.replace(pattern, "?*", ".+")
    pattern = StringUtil.replace(pattern, "*", ".*")
    pattern = StringUtil.replace(pattern, "?", ".")
    return pattern
}

private fun optimize(wildcardPattern: String): String {
    return wildcardPattern.replace("(?:\\.\\*)+".toRegex(), ".*")
}

private fun optimizeDirPattern(dirPattern: String): String {
    var pattern = dirPattern
    if (!pattern.startsWith("/")) {
        pattern = "/$pattern"
    }
    //now dirPattern starts and ends with '/'

    pattern = normalizeWildcards(pattern)

    pattern = StringUtil.replace(pattern, "/.*.*/", "(/.*)?/")
    pattern = StringUtil.trimEnd(pattern, "/")

    pattern = optimize(pattern)
    return pattern
}
