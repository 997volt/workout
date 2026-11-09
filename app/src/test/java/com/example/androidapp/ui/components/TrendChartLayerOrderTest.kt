package com.example.androidapp.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The figure's paint order (N40): summaries behind the data, except the moving average on top. */
class TrendChartLayerOrderTest {

    @Test
    fun theMovingAverage_paintsAfterTheReadings() {
        assertThat(TREND_DRAW_ORDER.indexOf(TrendLayer.MOVING_AVERAGE))
            .isGreaterThan(TREND_DRAW_ORDER.indexOf(TrendLayer.READINGS))
    }

    @Test
    fun theReferenceLines_paintBeforeTheData() {
        assertThat(TREND_DRAW_ORDER.indexOf(TrendLayer.REFERENCES))
            .isLessThan(TREND_DRAW_ORDER.indexOf(TrendLayer.READINGS))
    }
}
