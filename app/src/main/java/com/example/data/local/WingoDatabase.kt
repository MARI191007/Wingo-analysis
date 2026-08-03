package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.PeriodRecord
import com.example.data.model.PredictionResult

@Database(
    entities = [PeriodRecord::class, PredictionResult::class],
    version = 1,
    exportSchema = false
)
abstract class WingoDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun predictionDao(): PredictionDao

    companion object {
        @Volatile
        private var INSTANCE: WingoDatabase? = null

        fun getInstance(context: Context): WingoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WingoDatabase::class.java,
                    "wingo_predictor.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
