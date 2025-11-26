package com.web.test.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.web.test.presentation.navigation.NavRoutes

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state) {
        when (val current = state) {
            SplashUiState.Loading -> Unit
            SplashUiState.NavigateToModerator -> {
                navController.navigate(NavRoutes.GAME) {
                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                }
            }
            is SplashUiState.NavigateToWebView -> {
                val encodedUrl = Uri.encode(current.url)
                navController.navigate("${NavRoutes.WEBVIEW}?url=$encodedUrl") {
                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                }
            }
            is SplashUiState.Error -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val current = state) {
            SplashUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            is SplashUiState.Error -> Text(text = current.message, color = MaterialTheme.colorScheme.error)
            else -> Unit
        }
    }
}
