package com.web.test.presentation.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
fun AppEntryScreen(
    navController: NavController,
    viewModel: AppEntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state) {
        when (val current = state) {
            is AppEntryUiState.NavigateToSplash -> {
                navController.navigate(NavRoutes.SPLASH) {
                    popUpTo(NavRoutes.ENTRY) { inclusive = true }
                }
            }
            is AppEntryUiState.NavigateToWebView -> {
                val encodedUrl = Uri.encode(current.url)
                navController.navigate("${NavRoutes.WEBVIEW}?url=$encodedUrl") {
                    popUpTo(NavRoutes.ENTRY) { inclusive = true }
                }
            }
            AppEntryUiState.Loading -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
