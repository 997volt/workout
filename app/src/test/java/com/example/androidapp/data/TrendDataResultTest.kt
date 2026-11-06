package com.example.androidapp.data

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** A cancelled trends collection is cancellation, not a storage failure (B367). */
class TrendDataResultTest {

    @Test
    fun aStorageFailure_becomesAValue() = runTest {
        val emitted = flow<Int> { throw IllegalStateException("db") }.toDataResult().toList()

        assertThat(emitted).hasSize(1)
        assertThat(emitted.single()).isInstanceOf(DataResult.Failure::class.java)
        assertThat((emitted.single() as DataResult.Failure).error)
            .isInstanceOf(DataError.Storage::class.java)
    }

    @Test
    fun aCancellation_isRethrown_ratherThanReported() = runTest {
        val thrown = runCatching {
            flow<Int> { throw CancellationException("screen gone") }.toDataResult().toList()
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
    }
}
