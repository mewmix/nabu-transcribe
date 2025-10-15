package com.k2fsa.sherpa.onnx.mysherpaapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.k2fsa.sherpa.onnx.mysherpaapp.data.SpeakerDatabase
import com.k2fsa.sherpa.onnx.mysherpaapp.viewmodels.MeetingListViewModel
import com.k2fsa.sherpa.onnx.mysherpaapp.viewmodels.MeetingListViewModelFactory

@Composable
fun MeetingListScreen(navController: NavController) {
    val context = LocalContext.current
    val database = SpeakerDatabase.getDatabase(context)
    val viewModel: MeetingListViewModel = viewModel(
        factory = MeetingListViewModelFactory(database)
    )
    val meetings by viewModel.meetings.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(meetings) { meeting ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { navController.navigate("meeting-details/${meeting.id}") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = meeting.title)
                    Text(text = meeting.tags.joinToString(", "))
                }
            }
        }
    }
}