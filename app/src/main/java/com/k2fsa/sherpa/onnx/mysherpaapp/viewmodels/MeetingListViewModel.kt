package com.k2fsa.sherpa.onnx.mysherpaapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k2fsa.sherpa.onnx.mysherpaapp.data.Meeting
import com.k2fsa.sherpa.onnx.mysherpaapp.data.SpeakerDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeetingListViewModel(private val database: SpeakerDatabase) : ViewModel() {

    private val _meetings = MutableStateFlow<List<Meeting>>(emptyList())
    val meetings: StateFlow<List<Meeting>> = _meetings

    init {
        viewModelScope.launch {
            database.meetingDao().getAll().collect {
                _meetings.value = it
            }
        }
    }
}

class MeetingListViewModelFactory(private val database: SpeakerDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeetingListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeetingListViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}