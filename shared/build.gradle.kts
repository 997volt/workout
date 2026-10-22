import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

/**
 * The program-edit core (ROADMAP N170).
 *
 * One pure implementation of the program document and the rules an edit obeys, compiled twice: the
 * `jvm` target is what `:app` links against, and the `js` target is what the import helper loads as
 * `shared.js`. Nothing here touches Room, file IO, Android or the DOM — that is what makes one
 * implementation possible, and what keeps the page's preview and the app's write agreeing.
 *
 * The JS artifact is a **classic script** on purpose: the helper opens from `file://`, where a module
 * script is blocked by CORS. `binaries.library()` writes it under `build/dist/js/`, and the helper's
 * copy is generated from there rather than hand-kept.
 */
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    js(IR) {
        // nodejs runs commonTest on the JS target; the library binary is the script the page loads.
        // The browser environment is deliberately not enabled: its test runner pulls a git-hosted
        // karma, which npm refuses to fetch, and the page only needs the library's plain scripts.
        nodejs()
        binaries.library()
        // The helper is opened as a file, so the bundle must not need a module loader.
        // (The library binary is UMD; this keeps the syntax at what every current browser reads.)
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

/**
 * Copies the page's copy of the core into `docs/shared/` (ROADMAP N170).
 *
 * The page loads the same five scripts the compiler wrote — the UMD wrapper of each names its
 * dependency as a global, so they load in order from `file://` without a module loader. The copy is
 * committed so a fresh checkout and GitHub Pages both have a working page; `docs`' own test page
 * checks the version the loaded core reports, which is the cheap signal that the copy is current.
 *
 *     ./gradlew :shared:syncSharedJs
 */
val syncSharedJs by tasks.registering(Copy::class) {
    dependsOn("jsProductionLibraryCompileSync")
    from(layout.buildDirectory.dir("compileSync/js/main/productionLibrary/kotlin")) {
        include("*.js")
    }
    into(rootProject.layout.projectDirectory.dir("docs/shared"))
}

/**
 * Static analysis (ROADMAP N179).
 *
 * The same config the app uses, and deliberately **not** the Compose ruleset: this module has no
 * Compose, and a ruleset that loaded nothing would still be a dependency to keep current. :shared
 * decides what an edit writes into the lifter's data, so the module the app calls was the one
 * nobody linted.
 */
detekt {
    buildUponDefaultConfig = true
    parallel = true
    // Only the deltas live here; detekt's defaults stay authoritative.
    // The app's config, then the overlay that tolerates its two plugin sections (see the file).
    config.setFrom(
        rootProject.files("config/detekt/detekt.yml", "config/detekt/detekt-shared.yml"),
    )
    // A multiplatform module keeps its code under `src/commonMain`, which the plugin's default
    // `src/main/java` does not reach: without this the task is NO-SOURCE and the gate is a task
    // that passes by finding nothing, which is the failure N179 was written about.
    source.setFrom(files("src/commonMain/kotlin", "src/commonTest/kotlin"))
}
