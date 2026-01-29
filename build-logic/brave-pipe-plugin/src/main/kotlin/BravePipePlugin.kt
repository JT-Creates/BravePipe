import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * This plugin is for BravePipe to change some source (java/res) files or generate xml files.
 *
 * It has the following features:
 * - BravePipe (all flavors): rename NewPipe to BravePipe for res/.../strings.xml
 * - BravePipe (all flavors): change support email address, update json update URL,
 *   adjust the apk signature
 *
 * - BravePipe (braveLegacy flavor): generate icons for drawable-night theme.
 * - BravePipe (braveLegacy flavor): remove duplicated files from main before compilation
 *
 * The tasks have to be called manually. See BravePipes readme file
 */
class BravePipePlugin : Plugin<Project> {
    override fun apply(project: Project) {

        // start -- 'main' sources tasks
        project.tasks.register(BraveTaskNames.BRAVIFY) {
            group = "brave"
            description = "Replaces 'NewPipe' with 'BravePipe' in strings.xml files"

            dependsOn(
                BraveTaskNames.REPLACE_NEWPIPE_STRINGS,
                BraveTaskNames.REPLACE_NEWPIPE_MAIL_AND_API_AND_SIGNATURE
            )
        }

        project.tasks.register<ReplaceStringsTask>(BraveTaskNames.REPLACE_NEWPIPE_STRINGS) {
            group = "brave"
            description = "Replaces 'NewPipe' with 'BravePipe' in strings.xml files"

            sourceDir.set(project.layout.projectDirectory.dir("src/main/res"))
            outputDir.set(project.layout.buildDirectory.dir("none"))
            replaceInPlace.set(true) // if true outputDir will be ignored
            dryRun.set(false)
        }

        project.tasks.register<AlterFilesTask>(BraveTaskNames.REPLACE_NEWPIPE_MAIL_AND_API_AND_SIGNATURE) {
            group = "brave"
            description = "altering specific Java/Kotlin files"

            sourceDir.set(project.layout.projectDirectory.dir("src/main/java")) // is not used
            targetDir.set(project.layout.projectDirectory.dir("src/main/java"))
            dryRun.set(false)
        }

        project.tasks.register<ReplaceStringsTask>(BraveTaskNames.TEST_REPLACE_NEWPIPE_STRINGS) {
            group = "braveTest"
            description = "Tests NewPipe' with 'BravePipe' in strings.xml files"

            sourceDir.set(project.layout.projectDirectory.dir("src/main/res"))
            outputDir.set(project.layout.buildDirectory.dir("tmp/test_strings_replaced"))
            replaceInPlace.set(false) // if false outputDir will be used
            dryRun.set(false)
        }
        // end -- 'main' sources tasks

        // start -- braveLegacy flavor tasks
        project.tasks.register(BraveTaskNames.PREPARE_LEGACY_FLAVOR) {
            group = "brave"
            description = "Prepares files for braveLegacy flavor"

            dependsOn(
                BraveTaskNames.LEGACY_FLAVOR_FILES_PREPARE
            )
        }

        project.tasks.register(BraveTaskNames.UNPREPARE_LEGACY_FLAVOR) {
            group = "brave"
            description = "Restores files after braveLegacy flavor preparation"

            dependsOn(
                BraveTaskNames.LEGACY_FLAVOR_FILES_UNPREPARE
            )
        }

        project.tasks.register<GenerateDrawableNightTask>(BraveTaskNames.TEST_LEGACY_FLAVOR_DRAWABLE_NIGHT_XMLS_CREATE) {
            group = "braveTest"
            description = "Generates drawable-night icons for settings main page"

            appDir.set(project.layout.projectDirectory.asFile)
            doRemove.set(false)
            dryRun.set(false)
        }

        project.tasks.register<GenerateDrawableNightTask>(BraveTaskNames.TEST_LEGACY_FLAVOR_DRAWABLE_NIGHT_XMLS_REMOVE) {
            group = "braveTest"
            description = "Removes generated drawable-night icons"
            appDir.set(project.layout.projectDirectory.asFile)
            doRemove.set(true)
            dryRun.set(false)
        }

        createBraveLegacyFileAlteringTasks(project)
        // end -- braveLegacy flavor tasks
    }

    private fun createBraveLegacyFileAlteringTasks(project: Project) {
        createBraveLegacyBasicDaoAlteringTask(
            false,
            project,
            BraveTaskNames.TEST_LEGACY_FLAVOR_DAO_PREPARE
        )
        createBraveLegacyBasicDaoAlteringTask(
            true,
            project,
            BraveTaskNames.TEST_LEGACY_FLAVOR_DAO_UNPREPARE
        )

        createBraveLegacyFileAlteringTask(
            false,
            project,
            BraveTaskNames.LEGACY_FLAVOR_FILES_PREPARE,
            "Prepares files for braveLegacy flavor by moving duplicates"
        )
        createBraveLegacyFileAlteringTask(
            true,
            project,
            BraveTaskNames.LEGACY_FLAVOR_FILES_UNPREPARE,
            "Restores files after braveLegacy flavor preparation"
        )
    }

    private fun createBraveLegacyBasicDaoAlteringTask(
        restoreIt: Boolean,
        project: Project,
        name: String
    ) {
        project.tasks.register<AlterBraveLegacyBasicDao>(name) {
            group = "bravetest"
            description = "altering specific Java/Kotlin files for braveLegacyFlavor"

            sourceDir.set(project.layout.projectDirectory.dir("src/main/java")) // is not used
            targetDir.set(project.layout.projectDirectory.dir("src/main/java"))
            appDir.set(project.layout.projectDirectory.asFile)
            doRestore.set(restoreIt)
            dryRun.set(false)
        }
    }

    private fun createBraveLegacyFileAlteringTask(
        restoreIt: Boolean,
        project: Project,
        name: String,
        desc: String
    ) {

        project.tasks.register<PrepareLegacyFlavorTask>(name) {
            group = "brave"
            description = desc

            sourceDir.set(project.layout.projectDirectory.dir("src/braveLegacy/java"))
            tempDir.set(project.layout.projectDirectory.dir("tmp-legacy"))
            doRestore.set(restoreIt)
            appDir.set(project.layout.projectDirectory.asFile)
            dryRun.set(false)
        }
    }
}
