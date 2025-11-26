package com.web.test.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.web.test.presentation.entry.AppEntryScreen
import com.web.test.presentation.game.GameScreen
import com.web.test.presentation.splash.SplashScreen
import com.web.test.presentation.webview.WebViewScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NavRoutes.ENTRY,
        modifier = modifier
    ) {
        composable(NavRoutes.ENTRY) {
            AppEntryScreen(navController)
        }
        composable(NavRoutes.SPLASH) {
            SplashScreen(navController)
        }
        composable(
            route = "${NavRoutes.WEBVIEW}?url={url}",
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            WebViewScreen()
        }
        composable(NavRoutes.GAME) {
            GameScreen()
        }
    }
}
