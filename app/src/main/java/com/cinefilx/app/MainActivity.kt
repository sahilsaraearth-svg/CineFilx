package com.cinefilx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cinefilx.app.ui.navigation.CineFilxNavGraph
import com.cinefilx.app.ui.navigation.Screen
import com.cinefilx.app.ui.screens.settings.ThemeMode
import com.cinefilx.app.ui.theme.CineFilxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            val darkTheme: Boolean? = when (themeMode) {
                ThemeMode.SYSTEM -> null
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
            }
            CineFilxTheme(darkTheme = darkTheme) {
                MainScreen(themeMode = themeMode, onThemeChange = { themeMode = it })
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MainScreen(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home.route,      "Home",      Icons.Filled.Home,            Icons.Outlined.Home),
        BottomNavItem(Screen.Explore.route,   "Explore",   Icons.Filled.Explore,         Icons.Outlined.Explore),
        BottomNavItem(Screen.Watchlist.route, "Watchlist", Icons.Filled.Bookmark,        Icons.Outlined.BookmarkBorder),
        BottomNavItem(Screen.Settings.route,  "Settings",  Icons.Filled.Settings,        Icons.Outlined.Settings)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        CineFilxNavGraph(
            navController = navController,
            modifier      = Modifier.padding(innerPadding),
            themeMode     = themeMode,
            onThemeChange = onThemeChange
        )
    }
}
