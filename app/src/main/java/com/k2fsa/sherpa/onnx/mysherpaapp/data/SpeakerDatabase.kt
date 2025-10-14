package com.k2fsa.sherpa.onnx.mysherpaapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Speaker::class, Meeting::class, Turn::class], version = 1)
@TypeConverters(Converters::class)
abstract class SpeakerDatabase : RoomDatabase() {
    abstract fun speakerDao(): SpeakerDao
    abstract fun meetingDao(): MeetingDao
    abstract fun turnDao(): TurnDao

    companion object {
        @Volatile
        private var INSTANCE: SpeakerDatabase? = null

        fun getDatabase(context: Context): SpeakerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpeakerDatabase::class.java,
                    "speaker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}