package com.k2fsa.sherpa.onnx.mysherpaapp.export

import com.k2fsa.sherpa.onnx.mysherpaapp.data.Turn

class VttExporter {
    fun export(turns: List<Turn>): String {
        val sb = StringBuilder()
        sb.append("WEBVTT\n\n")
        turns.forEach { turn ->
            sb.append(formatTime(turn.startMs))
            sb.append(" --> ")
            sb.append(formatTime(turn.endMs))
            sb.append("\n")
            sb.append(turn.text)
            sb.append("\n\n")
        }
        return sb.toString()
    }

    private fun formatTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val milliseconds = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds)
    }
}
