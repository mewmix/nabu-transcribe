package com.k2fsa.sherpa.onnx.mysherpaapp

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object Help : NavRoutes("help")
    object Enroll : NavRoutes("enroll")
    object MeetingList : NavRoutes("meeting-list")
    object MeetingDetails : NavRoutes("meeting-details/{meetingId}")
}