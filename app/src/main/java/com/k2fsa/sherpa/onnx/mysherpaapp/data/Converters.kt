package com.k2fsa.sherpa.onnx.mysherpaapp.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun fromFloatArray(value: String): FloatArray {
        if (value.isBlank()) return FloatArray(0)
        return value.split(",").mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toFloatOrNull() }
            .toFloatArray()
    }

    @TypeConverter
    fun floatArrayToString(array: FloatArray): String {
        return array.joinToString(",")
    }
}