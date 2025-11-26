package com.web.test.presentation.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.test.domain.model.DecisionInput
import com.web.test.domain.usecase.CollectCloakInfoUseCase
import com.web.test.domain.usecase.GetDecisionUseCase
import com.web.test.domain.usecase.SaveDecisionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SplashUiState {
    object Loading : SplashUiState()
    object NavigateToModerator : SplashUiState()
    data class NavigateToWebView(val url: String) : SplashUiState()
    data class Error(val message: String) : SplashUiState()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val collectCloakInfoUseCase: CollectCloakInfoUseCase,
    private val getDecisionUseCase: GetDecisionUseCase,
    private val saveDecisionUseCase: SaveDecisionUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "SplashViewModel"
    }

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState

    init {
        resolveDecision()
    }

    fun resolveDecision() {
        viewModelScope.launch {
            try {
                _uiState.value = SplashUiState.Loading
                val cloakInfo = collectCloakInfoUseCase()
                Log.d(TAG, "Cloak info collected: $cloakInfo")
                val decision = getDecisionUseCase(DecisionInput(cloakInfo))
                saveDecisionUseCase(decision)

                if (decision.isModerator) {
                    _uiState.value = SplashUiState.NavigateToModerator
                } else {
                    val url = decision.targetUrl
                    if (!url.isNullOrBlank()) {
                        _uiState.value = SplashUiState.NavigateToWebView(url)
                    } else {
                        _uiState.value = SplashUiState.Error("No URL provided")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SplashUiState.Error(e.message ?: "Unexpected error")
            }
        }
    }
}