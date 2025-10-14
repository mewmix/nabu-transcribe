package com.k2fsa.sherpa.onnx.mysherpaapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.k2fsa.sherpa.onnx.mysherpaapp.data.Meeting
import com.k2fsa.sherpa.onnx.mysherpaapp.data.SpeakerDatabase
import com.k2fsa.sherpa.onnx.mysherpaapp.data.Turn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeetingDetailsViewModel(private val database: SpeakerDatabase, private val meetingId: Int) : ViewModel() {

    private val _meeting = MutableStateFlow<Meeting?>(null)
    val meeting: StateFlow<Meeting?> = _meeting

    private val _turns = MutableStateFlow<List<Turn>>(emptyList())
    val turns: StateFlow<List<Turn>> = _turns

    init {
        viewModelScope.launch {
            _meeting.value = database.meetingDao().getById(meetingId)
            database.turnDao().getByMeetingId(meetingId).collect {
                _turns.value = it
            }
        }
    }
}

class MeetingDetailsViewModelFactory(private val database: SpeakerDatabase, private val meetingId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeetingDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeetingDetailsViewModel(database, meetingId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}