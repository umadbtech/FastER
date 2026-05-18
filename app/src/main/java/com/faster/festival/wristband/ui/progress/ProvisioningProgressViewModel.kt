package com.faster.festival.wristband.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.wristband.domain.model.ProvisioningProgress
import com.faster.festival.wristband.domain.usecase.ProvisionWristbandUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns ALL real BLE Mesh provisioning state. Kept strictly separate from the
 * walkthrough's [com.faster.festival.ui.viewmodel.ProvisionViewModel] so the
 * marketing UI cannot accidentally reach into mesh state.
 */
class ProvisioningProgressViewModel(
    private val provision: ProvisionWristbandUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProvisioningProgress())
    val state: StateFlow<ProvisioningProgress> = _state.asStateFlow()
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = viewModelScope.launch {
            provision().collect { _state.value = it }
        }
    }

    fun retry() = start()

    fun cancel() {
        job?.cancel()
        _state.value = ProvisioningProgress()
    }

    class Factory(private val provision: ProvisionWristbandUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProvisioningProgressViewModel(provision) as T
    }
}
