package com.example.advent_11.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatThreadEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
