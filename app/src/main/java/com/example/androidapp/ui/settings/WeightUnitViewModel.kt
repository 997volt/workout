package com.example.androidapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The app-wide weight unit, for the shell to provide to every screen (ROADMAP N64).
 *
 * Its own ViewModel rather than a field on some screen's: the value is needed above the navigation
 * graph, where the screens that own the other settings are not composed.
 */
@HiltViewModel
class WeightUnitViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /**
     * Kilograms until the stored preference arrives, which is the value it would have anyway when
     * none is stored — so the first frame is never a flash of the wrong unit.
     */
    val weightUnit: StateFlow<WeightUnit> = settingsRepository.observeWeightUnit()
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KILOGRAMS)
}
