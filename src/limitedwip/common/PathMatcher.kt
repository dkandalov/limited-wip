package limitedwip.common

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.apache.oro.text.regex.Perl5Compiler

data class PathMatcher(
    val fileNameRegex: Regex,
    val dirRegex: Regex?
) {
    fun matches(filePath: String): Boolean {
        val slashIndex = filePath.lastIndexOf('/')
        return if (slashIndex >= 0) {
            val dir = filePath.substring(0, slashIndex)
            val fileName = filePath.substring(slashIndex + 1)
            fileNameRegex.matches(fileName) && (dirRegex == null || dirRegex.matches(dir))
        } else {
            fileNameRegex.matches(filePath)
        }
    }

    companion object {
        // Based on org.jetbrains.jps.model.java.impl.compiler.ResourcePatterns.convertToRegexp.
        // Could use `FileSystems.getDefault().getPathMatcher()` with glob but it'll be OS specific
        // and would be more complicated rules than the ones below.
        fun parse(wildcardPattern: String): PathMatcher {
            var pattern = FileUtil.toSystemIndependentName(wildcardPattern)

            var dirPattern: String? = null
            val slash = pattern.lastIndexOf('/')
            if (slash >= 0) {
                dirPattern = pattern.substring(0, slash)
                pattern = pattern.substring(slash + 1)
                dirPattern = optimizeDirPattern(dirPattern)
            }

            pattern = normalizeWildcards(pattern)
            pattern = optimize(pattern)

            val dirRegex = dirPattern?.compilePattern().ifNotNull { Regex(it) }
            val fileNameRegex = Regex(pattern.compilePattern())
            return PathMatcher(fileNameRegex, dirRegex)
        }
    }
}

fun String.toPathMatchers(): Set<PathMatcher> =
    split(';').mapTo(HashSet()) { PathMatcher.parse(it) }

private fun String.compilePattern(): String =
    try {
        val compiler = Perl5Compiler()
        if (SystemInfo.isFileSystemCaseSensitive) compiler.compile(this).pattern
        else compiler.compile(this, Perl5Compiler.CASE_INSENSITIVE_MASK).pattern
    } catch (ex: org.apache.oro.text.regex.MalformedPatternException) {
        error(ex.message.toString())
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

private fun optimize(wildcardPattern: String) = wildcardPattern.replace("(?:\\.\\*)+".toRegex(), ".*")
