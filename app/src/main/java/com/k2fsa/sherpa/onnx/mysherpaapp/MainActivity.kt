package com.k2fsa.sherpa.onnx.mysherpaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.k2fsa.sherpa.onnx.mysherpaapp.screens.EnrollScreen
import com.k2fsa.sherpa.onnx.mysherpaapp.screens.HelpScreen
import com.k2fsa.sherpa.onnx.mysherpaapp.screens.HomeScreen
import com.k2fsa.sherpa.onnx.mysherpaapp.screens.MeetingDetailsScreen
import com.k2fsa.sherpa.onnx.mysherpaapp.screens.MeetingListScreen
import com.k2fsa.sherpa.onnx.mysherpaapp.ui.theme.SherpaOnnxSpeakerDiarizationTheme
import com.k2fsa.sherpa.onnx.mysherpaapp.utils.AppLog

const val TAG = "sherpa-onnx-app"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            SherpaOnnxEngine.init(assets)
        } catch (e: Exception) {
            AppLog.e("Failed to initialize SherpaOnnxEngine", e)
            // TODO: Show a dialog to the user
        }
        setContent {
            SherpaOnnxSpeakerDiarizationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.MeetingList.route) { MeetingListScreen(navController) }
            composable(Screen.Enroll.route) { EnrollScreen() }
            composable(Screen.Help.route) { HelpScreen() }
            composable(
                "meeting_details/{meetingId}",
                arguments = listOf(navArgument("meetingId") { type = NavType.IntType })
            ) { backStackEntry ->
                val meetingId = backStackEntry.arguments?.getInt("meetingId")
                meetingId?.let {
                    MeetingDetailsScreen(meetingId = it)
                }
            }
        }
    }
}

@Composable
fun BottomBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Home,
        Screen.MeetingList,
        Screen.Enroll,
        Screen.Help,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                label = { Text(text = screen.title) },
                icon = { Icon(imageVector = screen.icon, contentDescription = "Navigation icon") },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object MeetingList : Screen("meeting_list", "Meetings", Icons.Filled.List)
    object Enroll : Screen("enroll", "Enroll", Icons.Filled.Person)
    object Help : Screen("help", "Help", Icons.Filled.Info)
}
