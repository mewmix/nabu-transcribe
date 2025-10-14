package com.k2fsa.sherpa.onnx.mysherpaapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List

object NavBarItems {
    val BarItems = listOf(
        BarItem(
            title = "Home",
            image = Icons.Filled.Home,
            route = "home",
        ),
        BarItem(
            title = "Meetings",
            image = Icons.Filled.List,
            route = "meeting-list",
        ),
        BarItem(
            title = "Help",
            image = Icons.Filled.Info,
            route = "help",
        ),
    )
}