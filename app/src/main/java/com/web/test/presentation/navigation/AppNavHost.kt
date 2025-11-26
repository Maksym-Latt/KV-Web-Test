package com.web.test.presentation.navigation

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.web.test.presentation.screens.GameScreen
import com.web.test.presentation.screens.SplashScreen
import com.web.test.presentation.screens.WebViewScreen
import com.web.test.presentation.viewmodel.SplashViewModel
import com.web.test.presentation.viewmodel.WebViewViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    startUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val webViewRoute = "${AppDestinations.WebView}/{url}"
    val initialDestination = if (startDestination == AppDestinations.WebView && !startUrl.isNullOrBlank()) {
        "${AppDestinations.WebView}/$startUrl"
    } else {
        startDestination
    }

    NavHost(
        navController = navController,
        startDestination = initialDestination,
        modifier = modifier,
    ) {
        composable(AppDestinations.Splash) {
            val viewModel: SplashViewModel = hiltViewModel()
            SplashScreen(
                viewModel = viewModel,
                navigateToGame = {
                    navController.navigate(AppDestinations.Game) {
                        popUpTo(0)
                    }
                },
                navigateToWebView = { url ->
                    navController.navigate("${AppDestinations.WebView}/${Uri.encode(url)}") {
                        popUpTo(0)
                    }
                },
            )
        }
        composable(AppDestinations.Game) {
            GameScreen()
        }
        composable(
            route = webViewRoute,
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    defaultValue = startUrl ?: ""
                },
            ),
        ) {
            val viewModel: WebViewViewModel = hiltViewModel()
            WebViewScreen(url = viewModel.targetUrl)
        }
    }
}
