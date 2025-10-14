package com.k2fsa.sherpa.onnx.mysherpaapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val startedAt: Long = System.currentTimeMillis(),
    var endedAt: Long? = null,
    val tags: List<String> = emptyList()
)