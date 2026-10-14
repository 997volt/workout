package com.example.androidapp

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * The window setting `enableEdgeToEdge()` does not make for itself (B127).
 *
 * `imePadding()` is only the **single** owner of the keyboard inset while the window is not also free to
 * pan. Left at the platform default, the resolved mode is `adjustPan`: measured on `pixel6_api36`, the
 * running workout's window reported `sim={adjust=pan}` and panned by an amount that depends on the focused
 * field, while `imePadding()` independently reserved the keyboard's 901 px. Two mechanisms, one of them
 * focus-dependent, is what a black band that changes size between attempts *is*.
 *
 * Nothing else in the build can notice this going missing again — `enableEdgeToEdge()` never touches the
 * soft input mode, and the Compose rule lint would need does not exist — so it is asserted here, the way
 * [DocsConsistencyTest] asserts the CI task set rather than trusting it.
 */
class EdgeToEdgeWindowTest {

    @Test
    fun the_activity_resizes_for_the_keyboard_rather_than_panning() {
        val manifest = File(repoRoot, "app/src/main/AndroidManifest.xml").readText()

        assertThat(manifest).contains("android:windowSoftInputMode=\"adjustResize\"")
    }
}
