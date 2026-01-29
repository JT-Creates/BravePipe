import java.io.File

class BraveRegexHelper {
    enum class RegexFlag(val regexOption: RegexOption) {
        /** ^ and $ match per line */
        M(RegexOption.MULTILINE),

        /** . matches newline */
        S(RegexOption.DOT_MATCHES_ALL),

        /** case-insensitive */
        I(RegexOption.IGNORE_CASE),

        /** match literal */
        L(RegexOption.LITERAL)
    }

    private fun Set<RegexFlag>.toRegexOptions(): Set<RegexOption> =
        map { it.regexOption }.toSet()

    public fun replaceAndVerify(
        dir: File,
        fileName: String,
        match: String,
        replace: String,
        verify: String,
        byLine: Boolean,
        flags: Set<RegexFlag>
    ) {
        val file = File(dir, fileName)
        if (!file.exists()) throw IllegalStateException("File not found: $fileName")

        val text = file.readText()
        if (text.contains(verify)) {
            println("[BravePipe] Already changed $match in $fileName")
            return
        }

        val regex = Regex(match, flags.toRegexOptions())
        val newText = if (byLine) {
            file.readLines().joinToString("\n") { regex.replace(it, replace) } + "\n"
        } else {
            regex.replace(text, replace)
        }

        println("[BravePipe] string replacing in file: $fileName [What:] $match")

        file.writeText(newText)

        val updatedText = file.readText()
        if (!updatedText.contains(verify)) throw AssertionError("[BravePipe] Verification failed for: [match=\"$match\"] [verify=\"$verify\"] in $fileName")
    }
}
