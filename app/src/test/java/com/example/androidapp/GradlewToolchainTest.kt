package com.example.androidapp

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The gate on `gradlew`'s workspace bootstrap (B262).
 *
 * The script carries a local block that sources `tools/android-env.sh` and scopes `HOME` to
 * `.toolchain/home`, so `./gradlew <task>` works with nothing exported — which is what
 * `README.md` promises. A `gradle-wrapper` bump regenerates the script from the distribution,
 * and the 9.8.2 bump deleted that block silently while the README kept promising it. No job
 * could see the loss: CI has a writable `$HOME` and a JDK on `PATH`, and `.toolchain/` is
 * gitignored, so the deleted block was a no-op there. It is asserted here instead, in the pull
 * request that would delete it, rather than trusted to a procedure nobody re-reads.
 *
 * The failure messages say what to do, because whoever trips one is reading a dependabot diff
 * and has probably never seen this file: re-apply the block, never delete the test.
 */
class GradlewToolchainTest {

    private val wrapper = File(repoRoot, "gradlew")

    @Test
    fun the_wrapper_sources_the_workspace_environment() {
        val script = wrapper.readText()

        assertWithMessage(
            "gradlew no longer sources tools/android-env.sh. A gradle-wrapper bump regenerates " +
                "this script and drops the workspace bootstrap (B262): re-apply the block from " +
                "git history (`git show adf205b3:gradlew`) — do not delete this test.",
        ).that(script).contains(". \"\$APP_HOME/tools/android-env.sh\"")
        assertThat(File(repoRoot, "tools/android-env.sh").isFile).isTrue()
    }

    @Test
    fun the_bootstrap_scopes_home_to_the_toolchain_without_touching_a_real_one() {
        // adb's keys and the emulator's alike live in $HOME/.android, and the host's $HOME is not
        // writable here (or may be a real user's) — so the redirect is scoped to the build process.
        val script = wrapper.readText()

        assertWithMessage(
            "gradlew no longer scopes HOME to .toolchain/home (B262). Without it, " +
                "connectedDebugAndroidTest cannot write ~/.android/adbkey; with it scoped to the " +
                "whole shell, git would lose ~/.ssh.",
        ).that(script).contains("HOME=\"\$APP_HOME/.toolchain/home\"")
        // Still a no-op where there is no .toolchain/, which is every CI runner and a fresh clone.
        assertThat(script).contains("if [ -d \"\$APP_HOME/.toolchain\" ]")
    }

    @Test
    fun the_bootstrap_runs_before_gradle_does() {
        // Sourced after the exec would set a PATH nobody reads: the block has to precede the call.
        val lines = wrapper.readLines()
        val sourced = lines.indexOfFirst { it.contains(". \"\$APP_HOME/tools/android-env.sh\"") }
        val launched = lines.indexOfFirst { it.contains("exec \"\$JAVACMD\"") }

        assertThat(sourced).isAtLeast(0)
        assertThat(launched).isAtLeast(0)
        assertWithMessage("the bootstrap is sourced before `exec \"\$JAVACMD\"` (B262)")
            .that(sourced)
            .isLessThan(launched)
    }
}
