import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
}

/**
 * Static analysis gate (ROADMAP F12). Lint covers Android-specific issues;
 * detekt adds complexity/maintainability checks plus the Compose ruleset, which
 * catches things lint cannot (a composable taking a `Modifier` it never uses,
 * an unstable collection parameter forcing recomposition, and similar).
 */
detekt {
    buildUponDefaultConfig = true
    parallel = true
    // Only the deltas live here; detekt's defaults stay authoritative.
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

/**
 * Release signing material is deliberately outside version control:
 * `keystore.properties` is gitignored (see .gitignore), and a checkout or CI run
 * without it still builds — the release APK is simply left unsigned, which is
 * enough to prove R8 succeeds. Create the file with:
 *
 *   storeFile=../release.jks
 *   storePassword=...
 *   keyAlias=...
 *   keyPassword=...
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val releaseStoreFile: String? = keystoreProperties.getProperty("storeFile")

/** The app's version, kept in one file (ROADMAP F14). */
val versionProperties = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val hasReleaseSigning = releaseStoreFile != null

/**
 * Room exports its schema as JSON so migrations can be tested against a real
 * baseline (ROADMAP F5). The files under `app/schemas/` are committed on
 * purpose: they are the record of every shipped schema version.
 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.example.androidapp"
    compileSdk = 37

    defaultConfig {
        // The permanent install identity (ROADMAP F14).
        //
        // `io.github.997volt` was the natural choice — this project publishes from
        // github.com/997volt/workout — but a Java package segment may not begin with a
        // digit, so AAPT rejects it outright. Hence `volt997`: the same handle with
        // the digits moved so the segment is a legal identifier.
        //
        // Changeable with this one line until the first upload; after that it is
        // permanent (it is what Health Connect grants and any deep links bind to).
        // If the project ever owns a domain, its reverse-DNS belongs here instead.
        //
        // `namespace` above is left as-is on purpose: it only affects the generated
        // R class and source packages, is invisible to users, and moving it would
        // also rename the committed Room schema directory. That is a separate,
        // deliberate refactor rather than part of shipping.
        applicationId = "io.github.volt997.workout"
        // API 26 is the floor for the Health Connect client (ROADMAP P4.1) and
        // makes java.time available natively, so no core library desugaring is
        // needed. Raising this from 24 was a deliberate trade: see ROADMAP F9.
        minSdk = 26
        targetSdk = 37
        // Read from version.properties so the two values cannot drift apart and a
        // bump never means editing Kotlin. Missing values fail the build here
        // rather than shipping a silent default.
        versionCode = requireNotNull(versionProperties.getProperty("versionCode")) {
            "versionCode is missing from version.properties"
        }.toInt()
        versionName = requireNotNull(versionProperties.getProperty("versionName")) {
            "versionName is missing from version.properties"
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile.orEmpty())
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 on for the real build (ROADMAP F10). Shrinking is not just about
            // APK size: it is the step that can silently strip reflection-based
            // code, and this app leans on it in two places — Hilt's generated
            // components and kotlinx-serialization's generated route serializers.
            // Both ship consumer rules, so they are left to prove themselves in
            // the signed-release smoke test rather than papered over with blanket
            // -keep rules that would defeat shrinking.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        // Makes the exported schema JSON visible to instrumented migration tests.
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    testOptions {
        unitTests {
            // Robolectric needs the merged resources and manifest to inflate the
            // Compose host activity.
            isIncludeAndroidResources = true
        }
    }

    lint {
        // The CI gate (ROADMAP F12) only means something if warnings fail the
        // build. Version-freshness checks are excluded because they hit the
        // network and would fail for reasons unrelated to this code — a
        // dependency bump is a deliberate act, not a build error.
        abortOnError = true
        warningsAsErrors = true
        disable += setOf(
            "NewerVersionAvailable",
            "GradleDependency",
            "AndroidGradlePluginVersion",
        )
        // No `baseline = file(...)` here, deliberately.
        //
        // Wiring one looks harmless, but AGP *creates* the file when it is missing:
        // the first run that finds a warning writes it into the baseline, and every
        // run after that silently accepts it. That turns "fail on every warning"
        // into "fail once", which is the opposite of what this gate is for.
        //
        // If a warning ever genuinely has to be accepted, do it in two deliberate
        // steps: run `./gradlew updateLintBaseline`, review the diff it produces,
        // commit it, and only then add the `baseline = file("lint-baseline.xml")`
        // line above.
    }
}

/**
 * The formatting ruleset, loaded **only** by [detektFormattingCheck] (B289).
 *
 * `detektPlugins` applies the plugins to every Detekt task, so the Compose rules loaded into this one
 * too and the report carried a finding from the gate — exactly what its own comment says must not
 * happen, because a caller reading the report then cannot tell which rule spoke. This configuration
 * exists so that task can be given the one ruleset it checks.
 */
val detektFormattingRules: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Presentation layer: ViewModels + lifecycle-aware state collection (F3).
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation with type-safe routes (F2).
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    // The program-edit core (ROADMAP N170): the document model, its codec and the edit planner,
    // compiled once here and once for the import helper's page.
    implementation(project(":shared"))

    // Dependency injection (F4).
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Local persistence (F5).
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Truth replaces JUnit's `assertEquals(expected, actual)` where a test is being
    // written or touched: those two arguments are easy to swap and the failure message
    // does not say which value was which. Turbine makes a sequence of emissions
    // assertable, which a polled `.value` cannot express at all. Both are JVM-only.
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    // Compose UI tests run on the JVM under Robolectric, so a UI assertion costs
    // seconds instead of a 26-minute CI emulator cycle. The instrumented job stays
    // for anything that genuinely needs a device.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.robolectric)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    detektPlugins(libs.detekt.rules.compose)
    detektFormattingRules(libs.detekt.formatting)
    // N129: not loaded by the gate above — see detektFormattingCheck below.
    detektPlugins(libs.detekt.formatting)
}

/**
 * The formatting ruleset, over a **file list** rather than the tree (ROADMAP N129).
 *
 * B123 settled that `detekt-formatting` stays out of the gate: loading it reports findings across a
 * large part of the tree (DECISIONS.md's Process section carries the current count), and the
 * alternatives were a repository-wide reformat or a set of `active: false` entries — the silent
 * gate `detekt.yml`'s header forbids. Its revisit trigger is a
 * layout defect that ships because a review missed it, which is what the malformed `BalanceRow` line
 * was.
 *
 * This is that trigger answered without either rejected alternative. The rules load from a **second
 * config file** and only over the files named in `-PdetektFormattingFiles`, so nothing is switched
 * off and nothing is reformatted: the check sees what a change touches and leaves the rest of the
 * repository at the layout it already has. It is deliberately **not** a task the gate depends on —
 * `tools/format-changed.py` runs it over a diff and reads the report.
 *
 * With no `-PdetektFormattingFiles` it runs over the whole tree, which is the measurement that
 * decides whether a rule can be turned on at all: a rule whose every finding today is a defect worth
 * fixing goes into the config, and one that disagrees with settled layout stays out.
 */
val detektFormattingCheck by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    description = "Runs detekt-formatting over the files named in -PdetektFormattingFiles."
    group = "verification"
    setSource(
        files(
            rootProject.file("app/src/main/java"),
            rootProject.file("app/src/test/java"),
            rootProject.file("app/src/androidTest/java"),
        ),
    )
    config.setFrom(rootProject.files("config/detekt/detekt-formatting.yml"))
    // The formatting rules are inactive in detekt's own defaults, so the plugin's own configuration
    // has to be laid under this file for them to be active at all. **Only** this ruleset is on the
    // classpath (B289): the library rulesets are the gate's job, and this run's report must contain
    // formatting findings only, or a caller reading it cannot tell which rule spoke.
    pluginClasspath.setFrom(detektFormattingRules)
    buildUponDefaultConfig = true
    disableDefaultRuleSets = true
    // Off unless asked for: this task reports, and the caller decides. A rule that rewrites the file
    // under a check is a check whose report no longer describes what it read, which is why the script
    // only turns this on for its own `--fix`.
    autoCorrect = providers.gradleProperty("detektFormattingAutoCorrect").isPresent
    parallel = true

    val named = providers.gradleProperty("detektFormattingFiles").orNull
    if (!named.isNullOrBlank()) {
        setSource(named.split(",").map { rootProject.file(it) })
    }

    reports {
        html.required.set(false)
        xml.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
        txt.required.set(true)
        txt.outputLocation.set(
            rootProject.layout.buildDirectory.file("reports/detekt-formatting/report.txt"),
        )
    }
    // A report is the output here, so a finding is not a build failure: the caller reads the file.
    ignoreFailures = true
}



// Robolectric writes a download lock and caches SDK jars under the user's home
// directory, which is outside this workspace and therefore not writable here.
// Pointing the *test JVM's* home at the build directory keeps it inside the
// workspace and still lets Robolectric fetch what it needs.
tasks.withType<Test>().configureEach {
    val robolectricHome = layout.buildDirectory.dir("robolectric-home").get().asFile
    systemProperty("user.home", robolectricHome.absolutePath)
    // Robolectric creates a lock file directly under the home directory and does
    // not create the directory first, so it must already exist.
    doFirst { robolectricHome.mkdirs() }

    // DocsConsistencyTest reads these at runtime. Without declaring them, Gradle sees no
    // input change after a documentation edit and reports the task UP-TO-DATE or
    // FROM-CACHE, so the guard would not run on the very change it exists to check and a
    // cached pass from older prose would read as green.
    inputs.files(
        rootProject.file("AGENTS.md"),
        rootProject.file("README.md"),
        rootProject.file("DECISIONS.md"),
        rootProject.file("DECISIONS-EVIDENCE.md"),
        rootProject.file("ROADMAP.md"),
        rootProject.file("CHANGELOG.md"),
        rootProject.file("CHANGELOG-1.x.md"),
        rootProject.file("IMPORT-HELPER.md"),
        rootProject.file("RELEASING.md"),
        rootProject.file(".github/workflows/android.yml"),
        rootProject.file(".dsh/skills/android-device-loop/SKILL.md"),
    ).withPropertyName("documentation").withPathSensitivity(PathSensitivity.RELATIVE)

    // GradlewToolchainTest reads the wrapper and the helper it sources, for the same reason: a
    // gradle-wrapper bump rewrites `gradlew` and nothing else in the build graph, so without this
    // the guard that exists to catch the deleted bootstrap would be UP-TO-DATE on exactly that
    // change and pass (B262).
    inputs.files(
        rootProject.file("gradlew"),
        rootProject.file("tools/android-env.sh"),
    ).withPropertyName("wrapper").withPathSensitivity(PathSensitivity.RELATIVE)
}
