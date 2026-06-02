package com.scanify.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scanify.app.data.local.dao.DocumentDao
import com.scanify.app.data.local.entity.DocumentEntity

@Database(entities = [DocumentEntity::class], version = 2, exportSchema = false)
abstract class DocumentDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}