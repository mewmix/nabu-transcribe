package com.k2fsa.sherpa.onnx.mysherpaapp.export

import com.k2fsa.sherpa.onnx.mysherpaapp.data.Meeting
import com.k2fsa.sherpa.onnx.mysherpaapp.data.Turn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JsonExporter {
    fun export(meeting: Meeting, turns: List<Turn>): String {
        val data = MeetingWithTurns(meeting, turns)
        return Json.encodeToString(data)
    }
}

@kotlinx.serialization.Serializable
data class MeetingWithTurns(val meeting: Meeting, val turns: List<Turn>)
