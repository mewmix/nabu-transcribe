package com.k2fsa.sherpa.onnx.mysherpaapp.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        if (value.isBlank()) {
            return emptyList()
        }
        return value.split(",").map { it.trim() }
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        if (list.isEmpty()) {
            return ""
        }
        return list.joinToString(",")
    }

    @TypeConverter
    fun fromFloatArray(embedding: FloatArray): String {
        if (embedding.isEmpty()) {
            return ""
        }
        return embedding.joinToString(separator = ",") { it.toString() }
    }

    @TypeConverter
    fun toFloatArray(serialized: String): FloatArray {
        if (serialized.isBlank()) {
            return FloatArray(0)
        }
        return serialized.split(",")
            .filter { it.isNotBlank() }
            .map { it.trim().toFloat() }
            .toFloatArray()
    }
}
