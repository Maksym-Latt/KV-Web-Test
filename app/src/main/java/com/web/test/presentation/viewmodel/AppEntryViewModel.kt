package com.web.test.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.test.domain.model.CachedDecision
import com.web.test.domain.usecase.GetCachedDecisionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AppEntryState {
    object Loading : AppEntryState()
    object Splash : AppEntryState()
    data class CachedWeb(val url: String) : AppEntryState()
}

@HiltViewModel
class AppEntryViewModel @Inject constructor(
    private val getCachedDecisionUseCase: GetCachedDecisionUseCase,
) : ViewModel() {

    private val _state: MutableStateFlow<AppEntryState> = MutableStateFlow(AppEntryState.Loading)
    val state: StateFlow<AppEntryState> = _state

    init {
        viewModelScope.launch {
            val cached = getCachedDecisionUseCase()
            handleCachedDecision(cached)
        }
    }

    private fun handleCachedDecision(cached: CachedDecision?) {
        val cachedUrl = cached?.cachedUrl
        _state.value = if (cachedUrl.isNullOrBlank()) {
            AppEntryState.Splash
        } else {
            AppEntryState.CachedWeb(cachedUrl)
        }
    }
}
