package com.web.test

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.web.test.presentation.navigation.AppDestinations
import com.web.test.presentation.navigation.AppNavHost
import com.web.test.presentation.viewmodel.AppEntryState
import com.web.test.presentation.viewmodel.AppEntryViewModel
import com.web.test.ui.theme.WebTestTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import android.net.Uri

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContent {
            WebTestTheme {
                val viewModel: AppEntryViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()
                AppContent(state = state)
            }
        }
    }
}

@Composable
private fun AppContent(state: AppEntryState) {
    when (state) {
        AppEntryState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is AppEntryState.CachedWeb -> {
            val navController = rememberNavController()
            AppNavHost(
                navController = navController,
                startDestination = AppDestinations.WebView,
                startUrl = Uri.encode(state.url),
            )
        }

        AppEntryState.Splash -> {
            val navController = rememberNavController()
            AppNavHost(
                navController = navController,
                startDestination = AppDestinations.Splash,
            )
        }
    }
}
