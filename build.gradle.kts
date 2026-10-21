// Top-level build file. Plugins are declared here and applied in the modules.
// AGP 9 has built-in Kotlin support, so no separate kotlin-android plugin is needed.
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
}

/**
 * The shared module's JS target applies the node plugin at the root (ROADMAP N170).
 *
 * Its default is to download a Node distribution, which it does by adding a **project** repository —
 * and these settings refuse project repositories on purpose. The machine already has node and npm, so
 * the download is turned off instead of the guard being loosened; `kotlin.js.yarn=false` in
 * `gradle.properties` picks npm for the same reason.
 */
plugins.withType<NodeJsRootPlugin> {
    @Suppress("DEPRECATION_ERROR")
    the<NodeJsRootExtension>().download = false
}

