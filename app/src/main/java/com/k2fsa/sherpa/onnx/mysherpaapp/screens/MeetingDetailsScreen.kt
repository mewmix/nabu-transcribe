package com.k2fsa.sherpa.onnx.mysherpaapp.screens

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
import com.k2fsa.sherpa.onnx.mysherpaapp.data.SpeakerDatabase
import com.k2fsa.sherpa.onnx.mysherpaapp.viewmodels.MeetingDetailsViewModel
import com.k2fsa.sherpa.onnx.mysherpaapp.viewmodels.MeetingDetailsViewModelFactory

@Composable
fun MeetingDetailsScreen(meetingId: Int) {
    val context = LocalContext.current
    val database = SpeakerDatabase.getDatabase(context)
    val viewModel: MeetingDetailsViewModel = viewModel(
        factory = MeetingDetailsViewModelFactory(database, meetingId)
    )
    val meeting by viewModel.meeting.collectAsState()
    val turns by viewModel.turns.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        meeting?.let {
            Text(text = it.title)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(turns) { turn ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Speaker: ${turn.speakerId ?: "Unknown"}")
                        Text(text = turn.text)
                    }
                }
            }
        }
    }
}