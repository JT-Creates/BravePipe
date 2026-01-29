import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.xml.parsers.DocumentBuilderFactory

class BraveHelpers(
    private val dryRun: Boolean
) {
    private val braveRegexHelper = BraveRegexHelper()

    companion object {
        val doNotReplaceLinesContaining = listOf(
            "name=\"donation_encouragement\"",
            "name=\"contribution_encouragement\"",
            "name=\"website_encouragement\"",
            "name=\"brave_about_fork\""
        )
    }

    fun shouldReplaceInLine(line: String): Boolean {
        return !doNotReplaceLinesContaining.any { line.contains(it) }
    }

    fun cleanupDir(dir: File?) {
        if (dir?.exists() == true) {
            dir.walkBottomUp().forEach { it.delete() }
        }
    }

    fun copyAndReplaceStrings(sourceDir: File, targetDir: File?, replaceInPlace: Boolean = false) {
        sourceDir.walkTopDown().forEach { srcFile ->
            if (srcFile.isFile) {
                if (srcFile.name == "strings.xml") {
                    val modified = replaceStrings(srcFile)
                    if (replaceInPlace) {
                        srcFile.writeText(modified.joinToString("\n") + "\n")
                    } else {
                        val tgtFile = prepareFoldersAndGetTargetFile(srcFile, sourceDir, targetDir)
                        tgtFile.writeText(modified.joinToString("\n") + "\n")
                    }
                } else if (!replaceInPlace) {
                    val tgtFile = prepareFoldersAndGetTargetFile(srcFile, sourceDir, targetDir)
                    Files.copy(
                        srcFile.toPath(),
                        tgtFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        }
    }

    private fun prepareFoldersAndGetTargetFile(
        srcFile: File,
        sourceDir: File,
        targetDir: File?
    ): File {
        val relativePath = srcFile.relativeTo(sourceDir)
        val tgtFile = File(targetDir, relativePath.path)
        tgtFile.parentFile.mkdirs()
        return tgtFile
    }

    private fun replaceStrings(srcFile: File): List<String> {
        val lines = srcFile.readLines()
        val modified = lines.map { line ->
            if (shouldReplaceInLine(line) && line.contains("NewPipe")) {

                line.replace("NewPipe", "BravePipe", ignoreCase = true)
                    .replace("নিউপাইপ", "ব্রেভপাইপ") // Bengali
                    .replace("نیوپایپ", "لوله شجاع") // Farsi
            } else {
                line
            }
        }
        return modified
    }

    fun alterFilesAndVerify(targetDir: File) {
        if (targetDir.absolutePath.contains("src/main/java")) {
            braveRegexHelper.replaceAndVerify(
                targetDir,
                "org/schabi/newpipe/error/ErrorActivity.java",
                "ERROR_EMAIL_ADDRESS = \"crashreport@newpipe.schabi.org\"",
                "ERROR_EMAIL_ADDRESS = \"crashreport@gmx.com\"",
                "crashreport@gmx.com",
                byLine = true,
                emptySet()
            )
            val braveIssueUrl =
                "https://github.com/bravenewpipe/NewPipeExtractor/issues"

            braveRegexHelper.replaceAndVerify(
                targetDir,
                "org/schabi/newpipe/error/ErrorActivity.java",
                "^(\\s*)(public static final String ERROR_GITHUB_ISSUE_URL =)\n[^;]*",
                "${'$'}1${'$'}2${'$'}1        \"${braveIssueUrl}\"",
                braveIssueUrl,
                byLine = false,
                setOf(BraveRegexHelper.RegexFlag.M)
            )
            braveRegexHelper.replaceAndVerify(
                targetDir,
                "org/schabi/newpipe/util/ReleaseVersionUtil.kt",
                "\"cb84069bd68116bafae5ee4ee5b08a567aa6d898404e7cb12f9e756df5cf5cab\"",
                "\"2f0c31d07f701416b2943376491cb16ebb718156defc2b1269aac04b94396c85\"",
                "\"2f0c31d07f701416b2943376491cb16ebb718156defc2b1269aac04b94396c85\"",
                byLine = true,
                emptySet()
            )
            val braveUpdateUrl =
                "https://raw.githubusercontent.com/bravepipeproject/bnp-r-mgr/master/api/data.json"
            braveRegexHelper.replaceAndVerify(
                targetDir,
                "org/schabi/newpipe/NewVersionWorker.kt",
                "^(\\s*)(private const val NEWPIPE_API_URL =).*$",
                "${'$'}1${'$'}2\n${'$'}1    \"${braveUpdateUrl}\"",
                "\"${braveUpdateUrl}\"",
                byLine = false,
                setOf(BraveRegexHelper.RegexFlag.M)
            )
        }
    }

}

/**
 * Helpers for building braveLegacy flavor of BravePipe.
 *
 * We have some same sourcecode files in 'main' and in 'braveLegacy' flavor.
 * To make it compile we need to remove the 'main' files first. Before calling:
 * # gradle assembleBraveLegacy{Debug,Release,Whatever}
 * you have to call:
 * $ gradle prepareLegacyFlavor
 * and afterwards to restore the files (in case you want to build another flavor:
 * $ gradle unPrepareLegacyFlavor
 */
class BraveLegacyHelpers(
    private val dryRun: Boolean,
    private val appDir: File
) {
    private val braveRegexHelper = BraveRegexHelper()

    companion object {

        const val BRAVE_LEGACY_FLAVOR = "braveLegacy"
        const val MAIN_FLAVOR = "main"
    }

    /**
     * move duplicated source files to a temporary directory
     *
     * We have some same sourcecode files in 'main' and in 'braveLegacy' flavor.
     * To make it compile we need to remove the 'main' files first.
     */
    fun prepareFilesForLegacy(sourceDir: File, tempDir: File, doRestore: Boolean) {
        sourceDir.walkTopDown()
            .filter { it.isFile && !it.name.startsWith("Brave") && (it.extension == "kt" || it.extension == "java") }
            .forEach { srcFile ->
                val relative = srcFile.relativeTo(sourceDir)
                val originFile = File(appDir, "src/$MAIN_FLAVOR/java/${relative.path}")
                val tempFile = File(tempDir, srcFile.name)
                if (doRestore) {
                    if (tempFile.exists()) {
                        if (!dryRun) {
                            Files.move(
                                tempFile.toPath(),
                                originFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                        println("[BraveLegacy] Restore: ${tempFile.absolutePath} -> ${originFile.absolutePath}")
                    }
                } else {
                    if (originFile.exists()) {
                        if (!dryRun) {
                            Files.move(
                                originFile.toPath(),
                                tempFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                        println("[BraveLegacy] Move: ${originFile.absolutePath} -> ${tempDir.absolutePath}")
                    }
                }
            }

        if (tempDir.list()?.isEmpty() == true) {
            tempDir.delete()
        }
    }

    /*
     * BravePipe braveLegacy flavor: add generating of icons for drawable-night to tasks [un]prepareLegacyFlavor.
     *
     * In main_settings.xml the icons did not show up if dark themes (-night -dark) are used.
     * below methods will generate -night version of this icons
     */

    fun legacyFlavorGenerateDrawableNightForMainSettings() {
        val destDir = File(appDir, "src/$BRAVE_LEGACY_FLAVOR/res/drawable-night")
        destDir.mkdirs()
        val drawables = generateListOfMainSettingsDrawableIconFileNames(appDir)
        drawables.forEach { drawableXml ->
            generateLegacyDrawableNightVersion(File(drawableXml), destDir)
        }
    }

    fun removeGeneratedDrawableNightForMainSettings() {
        val destDir = File(appDir, "src/${BRAVE_LEGACY_FLAVOR}/res/drawable-night")
        if (destDir.exists()) {
            val drawables = generateListOfMainSettingsDrawableIconFileNames(appDir)
            drawables.forEach { drawableXml ->
                removeLegacyDrawableNightVersion(File(drawableXml), destDir)
            }
            if (destDir.list()?.isEmpty() == true) {
                destDir.delete()
            }
        }
    }

    private fun generateListOfMainSettingsDrawableIconFileNames(appDir: File): List<String> {
        val mainRes = File(appDir, "src/$MAIN_FLAVOR/res")
        val mainSettingsXml = File(mainRes, "xml/main_settings.xml")
        val doc: Document =
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(mainSettingsXml)
        val prefs = doc.getElementsByTagName("PreferenceScreen")
        val drawables = mutableListOf<String>()
        for (i in 0 until prefs.length) {
            val icon = (prefs.item(i) as Element).getAttribute("android:icon")
            if (icon.isNotEmpty()) {
                val drawablePath = icon.replace("@", "$mainRes/")
                drawables.add("$drawablePath.xml")
            }
        }
        return drawables
    }

    private fun generateLegacyDrawableNightVersion(drawableXml: File, destDir: File) {
        val doc: Document =
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(drawableXml)
        doc.documentElement.setAttribute("android:tint", "#FFFFFF")
        val transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer()
        val outputFile = File(destDir, drawableXml.name)
        if (!dryRun) {
            transformer.transform(
                javax.xml.transform.dom.DOMSource(doc),
                javax.xml.transform.stream.StreamResult(outputFile)
            )
        }
        println("[BraveLegacy] Generated: ${outputFile.absolutePath}")
    }

    private fun removeLegacyDrawableNightVersion(drawableXml: File, destDir: File) {
        val xmlIconFile = File(destDir, drawableXml.name)
        if (xmlIconFile.exists()) {
            val deleted = if (!dryRun) xmlIconFile.delete() else false
            println("[BraveLegacy] Deleted ${xmlIconFile.name}: $deleted")
        }
    }

    /**
     * make java compiler happy using room 2.6.1.
     *
     * The last minSdk 19 room version 2.6.1 does not generate correct java code if the input
     * files are Kotlin and a Collection is used. With 'out' keyword it works.
     *
     * -    fun insertAll(entities: Collection<Entity>): List<Long>
     * +    fun insertAll(entities: Collection<out Entity>): List<Long>
     *
     * -> lets replace that for building the braveLegacy flavor
     */
    fun alterDaoSourceFiles(
        targetDir: File,
        doPrepare: Boolean
    ) {
        if (targetDir.absolutePath.contains("src/main/java")) {

            val findStrings = genBasicDAOReplacePattern(doPrepare)
            val verifyStrings = genBasicDAOReplacePattern(!doPrepare)

            for (x in 0 until findStrings.size) {
                val findString = findStrings.elementAt(x)
                val verifyString = verifyStrings.elementAt(x)

                braveRegexHelper.replaceAndVerify(
                    targetDir,
                    "org/schabi/newpipe/database/BasicDAO.kt",
                    findString,
                    verifyString,
                    verifyString,
                    byLine = false,
                    setOf(BraveRegexHelper.RegexFlag.L)
                )
            }

        }
    }

    private fun genBasicDAOReplacePattern(doPrepare: Boolean): Set<String> {
        val matchThat = if (doPrepare) "out " else ""
        val verify = setOf(
            "fun insertAll(entities: Collection<${matchThat}Entity>): List<Long>",
            "fun update(entities: Collection<${matchThat}Entity>)"
        )
        return verify
    }
}
