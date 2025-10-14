package com.k2fsa.sherpa.onnx.mysherpaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import com.k2fsa.sherpa.onnx.mysherpaapp.screens.MeetingListScreen
import com.k2fsa.sherpa.onnx.mysherpaapp.screens.MeetingDetailsScreen
import com.k2fsa.sherpa.onnx.mysherpaapp.ui.theme.SherpaOnnxSpeakerDiarizationTheme
import com.k2fsa.sherpa.onnx.mysherpaapp.utils.AppLog

const val TAG = "sherpa-onnx-app"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            SherpaOnnxEngine.init(assetManager)
        } catch (e: Exception) {
            AppLog.e("Failed to initialize SherpaOnnxEngine", e)
            // TODO: Show a dialog to the user
        }
        setContent {