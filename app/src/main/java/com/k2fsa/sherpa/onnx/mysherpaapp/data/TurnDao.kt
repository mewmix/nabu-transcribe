package com.k2fsa.sherpa.onnx.mysherpaapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TurnDao {
    @Insert
    suspend fun insert(turn: Turn)

    @Query("SELECT * FROM turns WHERE meetingId = :meetingId")
    suspend fun getByMeetingId(meetingId: Int): List<Turn>
}