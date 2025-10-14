package com.k2fsa.sherpa.onnx.mysherpaapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "turns")
data class Turn(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val meetingId: Int,
    val speakerId: Int?,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val conf: Float,
    val tags: List<String> = emptyList()
)