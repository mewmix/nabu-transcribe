package com.k2fsa.sherpa.onnx.mysherpaapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TurnDao {
    @Insert
    suspend fun insert(turn: Turn)

    @Query("SELECT * FROM turns WHERE meetingId = :meetingId")
    fun getByMeetingId(meetingId: Int): Flow<List<Turn>>
}