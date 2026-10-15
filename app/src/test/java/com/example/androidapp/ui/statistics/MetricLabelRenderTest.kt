package com.example.androidapp.ui.statistics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The picker's labels as a lifter reads them (ROADMAP B137).
 *
 * `MetricRegistryTest` compares a label by its **source** — a string resource or the taxonomy's own
 * word — which cannot see that the tape site *Chest* and the soreness muscle *Chest* both render
 * "Chest". That is exactly how two identical entries came to sit in one group with nothing to tell them
 * apart, so this resolves the resources and compares what is actually drawn. `AndroidJUnit4` delegates
 * to Robolectric on the JVM, so reading a string resource costs no device.
 */
@RunWith(AndroidJUnit4::class)
class MetricLabelRenderTest {

    @Test
    fun everyGroup_rendersLabelsThatAreDistinct() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        MetricGroup.entries.forEach { group ->
            val rendered = MetricRegistry.entries
                .filter { it.group == group }
                .map { it.labelText ?: context.getString(requireNotNull(it.labelRes)) }

            assertWithMessage("two series in $group render the same word, so one is unreadable")
                .that(rendered.distinct().size)
                .isEqualTo(rendered.size)
        }
    }
}
