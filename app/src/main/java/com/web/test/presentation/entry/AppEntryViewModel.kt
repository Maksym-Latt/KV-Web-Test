package com.web.test.presentation.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.test.domain.usecase.GetCachedDecisionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AppEntryUiState {
    data class NavigateToWebView(val url: String) : AppEntryUiState()
    object NavigateToSplash : AppEntryUiState()
    object Loading : AppEntryUiState()
}

@HiltViewModel
class AppEntryViewModel @Inject constructor(
    private val getCachedDecisionUseCase: GetCachedDecisionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppEntryUiState>(AppEntryUiState.Loading)
    val uiState: StateFlow<AppEntryUiState> = _uiState

    init {
        viewModelScope.launch {
            val cached = getCachedDecisionUseCase()
            val cachedUrl = cached?.cachedUrl
            if (!cachedUrl.isNullOrBlank()) {
                _uiState.value = AppEntryUiState.NavigateToWebView(cachedUrl)
            } else {
                _uiState.value = AppEntryUiState.NavigateToSplash
            }
        }
    }
}
