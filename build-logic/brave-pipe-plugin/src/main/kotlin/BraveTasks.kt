import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * this are the names all task will be registered within gradle
 */
object BraveTaskNames {
    /* tasks that the user should use */
    const val BRAVIFY = "bravify"
    const val PREPARE_LEGACY_FLAVOR = "prepareLegacyFlavor"
    const val UNPREPARE_LEGACY_FLAVOR = "unprepareLegacyFlavor"

    /* replace the strings and crashreport mail, update api and signature */
    const val REPLACE_NEWPIPE_STRINGS = "z_taskReplaceNewPipeStringsXml"
    const val REPLACE_NEWPIPE_MAIL_AND_API_AND_SIGNATURE =
        "z_taskReplaceNewPipeMailandJsonandSignature"

    const val LEGACY_FLAVOR_FILES_PREPARE = "z_taskLegacyFlavorFilesPrepare"
    const val LEGACY_FLAVOR_FILES_UNPREPARE = "z_taskLegacyFlavorFilesUnprepare"

    /* tasks to test a single feature. This features are all called either in
     * BRAVIFY or UNPREPARE_LEGACY_FLAVOR or PREPARE_LEGACY_FLAVOR */
    const val TEST_REPLACE_NEWPIPE_STRINGS = "testReplaceNewPipeStrings"
    const val TEST_LEGACY_FLAVOR_DRAWABLE_NIGHT_XMLS_CREATE =
        "testLegacyFlavorDrawableNightXmlsCreate"
    const val TEST_LEGACY_FLAVOR_DRAWABLE_NIGHT_XMLS_REMOVE =
        "testLegacyFlavorDrawableNightXmlsRemove"
    const val TEST_LEGACY_FLAVOR_DAO_PREPARE = "testLegacyFlavorDaoPrepare"
    const val TEST_LEGACY_FLAVOR_DAO_UNPREPARE = "testLegacyFlavorDaoUnprepare"
}


/**
 * if outputDir is null we replace in place
 */
abstract class ReplaceStringsTask : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /**
     * if used outputDir will not be used and we replace in the input files.
     */
    @get:Input
    abstract val replaceInPlace: Property<Boolean>

    @get:Input
    abstract val dryRun: Property<Boolean>

    @TaskAction
    fun execute() {
        val helpers = BraveHelpers(dryRun.get())
        val src = sourceDir.get().asFile
        val tgt = outputDir.get().asFile

        if (!replaceInPlace.get()) helpers.cleanupDir(tgt)
        helpers.copyAndReplaceStrings(src, tgt, replaceInPlace.get())
    }
}

abstract class AlterFilesTask : DefaultTask() {
    /**
     * we just have this InputDirectory to make the caching happy -- I guess
     */
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val targetDir: DirectoryProperty

    @get:Input
    abstract val dryRun: Property<Boolean>

    @TaskAction
    fun execute() {
        val helpers = BraveHelpers(dryRun.get())
        helpers.alterFilesAndVerify(targetDir.get().asFile)
    }
}

abstract class PrepareLegacyFlavorTask : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val tempDir: DirectoryProperty

    @get:Input
    abstract val doRestore: Property<Boolean>

    @get:InputDirectory
    abstract val appDir: DirectoryProperty

    @get:Input
    abstract val dryRun: Property<Boolean>

    @TaskAction
    fun execute() {
        val helpers = BraveLegacyHelpers(dryRun.get(), appDir.get().asFile)
        val src = sourceDir.get().asFile
        val tmp = tempDir.get().asFile

        helpers.prepareFilesForLegacy(src, tmp, doRestore.get())
        helpers.alterDaoSourceFiles(
            appDir.get().dir("src/main/java").asFile, doRestore.get()
        )
        if (doRestore.get()) {
            helpers.removeGeneratedDrawableNightForMainSettings()
        } else {
            helpers.legacyFlavorGenerateDrawableNightForMainSettings()
        }
    }
}

abstract class GenerateDrawableNightTask : DefaultTask() {
    @get:InputDirectory
    abstract val appDir: DirectoryProperty

    @get:Input
    abstract val doRemove: Property<Boolean>

    @get:Input
    abstract val dryRun: Property<Boolean>

    @TaskAction
    fun execute() {
        val helpers = BraveLegacyHelpers(dryRun.get(), appDir.get().asFile)
        if (doRemove.get()) {
            helpers.removeGeneratedDrawableNightForMainSettings()
        } else {
            helpers.legacyFlavorGenerateDrawableNightForMainSettings()
        }
    }
}

abstract class AlterBraveLegacyBasicDao : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val targetDir: DirectoryProperty

    @get:Input
    abstract val doRestore: Property<Boolean>

    @get:InputDirectory
    abstract val appDir: DirectoryProperty

    @get:Input
    abstract val dryRun: Property<Boolean>

    @TaskAction
    fun execute() {
        val helpers = BraveLegacyHelpers(dryRun.get(), appDir.get().asFile)
        val src = targetDir.get().asFile
        helpers.alterDaoSourceFiles(src, doRestore.get())
    }
}
