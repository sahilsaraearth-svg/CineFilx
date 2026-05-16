package com.cinefilx.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cinefilx.app.data.model.MediaType
import com.cinefilx.app.ui.screens.detail.DetailScreen
import com.cinefilx.app.ui.screens.explore.ExploreScreen
import com.cinefilx.app.ui.screens.home.HomeScreen
import com.cinefilx.app.ui.screens.settings.SettingsScreen
import com.cinefilx.app.ui.screens.settings.ThemeMode
import com.cinefilx.app.ui.screens.watchlist.WatchlistScreen

sealed class Screen(val route: String) {
    object Home      : Screen("home")
    object Explore   : Screen("explore")
    object Watchlist : Screen("watchlist")
    object Settings  : Screen("settings")
    object Detail    : Screen("detail/{mediaId}/{mediaType}") {
        fun createRoute(mediaId: Int, mediaType: MediaType) = "detail/$mediaId/${mediaType.value}"
    }
}

@Composable
fun CineFilxNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeChange: (ThemeMode) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(250)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(250))
        },
        exitTransition = {
            fadeOut(tween(200)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(200))
        },
        popEnterTransition = {
            fadeIn(tween(250)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(250))
        },
        popExitTransition = {
            fadeOut(tween(200)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(200))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(onMediaClick = { id, type -> navController.navigate(Screen.Detail.createRoute(id, type)) })
        }

        composable(Screen.Explore.route) {
            ExploreScreen(onMediaClick = { id, type -> navController.navigate(Screen.Detail.createRoute(id, type)) })
        }

        composable(Screen.Watchlist.route) {
            WatchlistScreen(onMediaClick = { id, type -> navController.navigate(Screen.Detail.createRoute(id, type)) })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(currentTheme = themeMode, onThemeChange = onThemeChange)
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("mediaId")   { type = NavType.IntType },
                navArgument("mediaType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaId      = backStackEntry.arguments?.getInt("mediaId") ?: 0
            val mediaTypeStr = backStackEntry.arguments?.getString("mediaType") ?: "movie"
            DetailScreen(
                mediaId     = mediaId,
                mediaType   = MediaType.fromValue(mediaTypeStr),
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
