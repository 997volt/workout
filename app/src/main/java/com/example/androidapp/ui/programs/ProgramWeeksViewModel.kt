package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.ZoneSource
import com.example.androidapp.domain.model.DeloadSuggestion
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.deloadSuggestion
import com.example.androidapp.domain.repository.AdherenceRepository
import com.example.androidapp.domain.repository.DeloadRepository
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.SettingsRepository
import com.example.androidapp.ui.navigation.ProgramWeeks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** One program's two week actions: exempt, or build a lighter week (ROADMAP N300). */
data class ProgramWeeksUiState(
    val isLoading: Boolean = true,
    val programName: String = "",
    val notFound: Boolean = false,
    val thisWeek: LocalDate? = null,
    val nextWeek: LocalDate? = null,
    val thisWeekExempt: Boolean = false,
    val nextWeekExempt: Boolean = false,
    val thisWeekHasPlan: Boolean = false,
    val nextWeekHasPlan: Boolean = false,
    val writing: Boolean = false,
    val error: DataError? = null,
    /** The cadence suggestion for this program, or null until the read lands (N301). */
    val suggestion: DeloadSuggestion? = null,
)

/**
 * The exemption and the deload are two actions (ROADMAP N300).
 *
 * The screen is deliberately small: the mark is one write, and the lighter week is the creator's. It
 * offers **next** week as well as this one because a vacation is known before it starts; the creator
 * already builds next week, and a later build reuses the mark it finds.
 */
@HiltViewModel
class ProgramWeeksViewModel @Inject constructor(
    private val programs: ProgramRepository,
    private val adherence: AdherenceRepository,
    private val deloads: DeloadRepository,
    private val settings: SettingsRepository,
    savedStateHandle: SavedStateHandle,
    timeSource: TimeSource,
    zoneSource: ZoneSource = ZoneSource { ZoneId.systemDefault() },
) : ViewModel() {

    private val programId: String = savedStateHandle.toRoute<ProgramWeeks>().programId

    private val zone = zoneSource.zone()
    private val thisWeek: LocalDate = ProgramSchedule.weekStartOf(
        timeSource.now().atZone(zone).toLocalDate(),
    )
    private val nextWeek: LocalDate = thisWeek.plusWeeks(1)

    private val writing = MutableStateFlow(false)
    private val error = MutableStateFlow<DataError?>(null)
    private val suggestion = MutableStateFlow<DeloadSuggestion?>(null)

    private val marks: kotlinx.coroutines.flow.Flow<Weeks> = combine(
        deloads.observeMarkedPrograms(thisWeek),
        deloads.observeMarkedPrograms(nextWeek),
        deloads.observeWeekPlans(thisWeek),
        deloads.observeWeekPlans(nextWeek),
    ) { thisMarked, nextMarked, thisPlans, nextPlans ->
        Weeks(
            thisExempt = programId in thisMarked,
            nextExempt = programId in nextMarked,
            thisPlan = thisPlans.any { it.programId == programId && it.days.isNotEmpty() },
            nextPlan = nextPlans.any { it.programId == programId && it.days.isNotEmpty() },
        )
    }

    init {
        // The suggestion counts marks, so it is re-read whenever one appears or goes — including the one
        // the creator writes for the week this screen just sent the lifter to build (B404).
        viewModelScope.launch { marks.collect { refreshSuggestion() } }
    }

    val uiState: StateFlow<ProgramWeeksUiState> = combine(
        programs.observeProgram(programId),
        marks,
        writing,
        error,
        suggestion,
    ) { program, weeks, isWriting, failure, due ->
        ProgramWeeksUiState(
            isLoading = false,
            programName = program?.name.orEmpty(),
            notFound = program == null,
            thisWeek = thisWeek,
            nextWeek = nextWeek,
            thisWeekExempt = weeks.thisExempt,
            nextWeekExempt = weeks.nextExempt,
            thisWeekHasPlan = weeks.thisPlan,
            nextWeekHasPlan = weeks.nextPlan,
            writing = isWriting,
            error = failure,
            suggestion = due,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ProgramWeeksUiState(),
    )

    /** Marks or unmarks one of the two weeks (ROADMAP N300). */
    fun onSetExempt(weekStart: LocalDate, exempt: Boolean) {
        if (writing.value) return
        viewModelScope.launch {
            writing.value = true
            // The suggestion follows the write through the live marks, not from here (B404).
            val result = adherence.setDeloadWeek(programId, weekStart, exempt)
            if (result is DataResult.Failure) error.value = result.error
            writing.value = false
        }
    }

    fun onErrorShown() {
        error.value = null
    }

    /**
     * The cadence suggestion: whole weeks since the last recovery week, or since the program started
     * (N301).
     *
     * One read per mark change rather than a live count: the base can be a mark from months ago, which
     * the two week flows on screen cannot see, so the collector that calls this is what makes it live
     * (B404). A program that is gone has no suggestion to make.
     */
    private suspend fun refreshSuggestion() {
        val cadence = settings.observeDeloadSuggestionWeeks().first()
        val program = programs.observeProgram(programId).first()
        if (program == null) {
            suggestion.value = null
            return
        }
        val started = program.startDate
            ?: program.createdAtMillis.takeIf { it > 0L }
                ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: thisWeek
        val last = deloads.findLastMark(programId, thisWeek.plusWeeks(2))
        suggestion.value = deloadSuggestion(
            cadenceWeeks = cadence,
            lastDeloadWeek = last,
            programStartedWeek = ProgramSchedule.weekStartOf(started),
            thisWeek = thisWeek,
        )
    }

    private data class Weeks(
        val thisExempt: Boolean,
        val nextExempt: Boolean,
        val thisPlan: Boolean,
        val nextPlan: Boolean,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
