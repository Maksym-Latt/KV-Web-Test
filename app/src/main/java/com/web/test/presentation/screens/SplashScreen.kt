package com.web.test.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.web.test.presentation.viewmodel.SplashUiState
import com.web.test.presentation.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    navigateToGame: () -> Unit,
    navigateToWebView: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startDecisionFlow()
    }

    LaunchedEffect(state) {
        when (state) {
            SplashUiState.NavigateToModerator -> navigateToGame()
            is SplashUiState.NavigateToWebView -> navigateToWebView(state.url)
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Web Test",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        when (state) {
            SplashUiState.Loading, SplashUiState.NavigateToModerator, is SplashUiState.NavigateToWebView -> {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }

            is SplashUiState.Error -> {
                Text(text = state.message, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
