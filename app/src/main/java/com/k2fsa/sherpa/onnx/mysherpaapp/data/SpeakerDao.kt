package com.k2fsa.sherpa.onnx.mysherpaapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SpeakerDao {
    @Insert
    suspend fun insert(speaker: Speaker)

    @Query("SELECT * FROM speakers")
    suspend fun getAll(): List<Speaker>

    @Query("SELECT * FROM speakers WHERE name = :name")
    suspend fun getByName(name: String): Speaker?

    @Query("DELETE FROM speakers WHERE id = :id")
    suspend fun delete(id: Int)
}