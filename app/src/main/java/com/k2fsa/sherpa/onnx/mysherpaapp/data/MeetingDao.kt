package com.k2fsa.sherpa.onnx.mysherpaapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MeetingDao {
    @Insert
    suspend fun insert(meeting: Meeting): Long

    @Query("SELECT * FROM meetings")
    suspend fun getAll(): List<Meeting>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getById(id: Int): Meeting?

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun delete(id: Int)
}