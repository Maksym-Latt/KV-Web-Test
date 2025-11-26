package com.web.test.presentation.viewmodel

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
    private val saveDecisionUseCase: SaveDecisionUseCase,
) : ViewModel() {

    private val _uiState: MutableStateFlow<SplashUiState> = MutableStateFlow(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState

    fun startDecisionFlow() {
        viewModelScope.launch {
            _uiState.value = SplashUiState.Loading
            runCatching {
                val cloakInfo = collectCloakInfoUseCase()
                val decision = getDecisionUseCase(DecisionInput(cloakInfo))
                saveDecisionUseCase(decision, System.currentTimeMillis())
                decision
            }.onSuccess { decision ->
                if (decision.isModerator) {
                    _uiState.value = SplashUiState.NavigateToModerator
                } else {
                    val target = decision.targetUrl
                    if (target.isNullOrBlank()) {
                        _uiState.value = SplashUiState.Error("Missing target url")
                    } else {
                        _uiState.value = SplashUiState.NavigateToWebView(target)
                    }
                }
            }.onFailure { throwable ->
                _uiState.value = SplashUiState.Error(throwable.message ?: "Unknown error")
            }
        }
    }
}
