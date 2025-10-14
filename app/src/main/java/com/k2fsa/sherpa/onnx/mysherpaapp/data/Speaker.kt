package com.k2fsa.sherpa.onnx.mysherpaapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "speakers")
data class Speaker(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val embedding: FloatArray,
    val createdAt: Long = System.currentTimeMillis()
)