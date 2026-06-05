package com.scanify.app.di

import android.content.Context
import androidx.room.Room
import com.scanify.app.data.backup.ExportStorageManager
import com.scanify.app.data.local.dao.DocumentDao
import com.scanify.app.data.local.database.DocumentDatabase
import com.scanify.app.domain.repository.DocumentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDocumentDatabase(@ApplicationContext context: Context): DocumentDatabase {
        return Room.databaseBuilder(
            context,
            DocumentDatabase::class.java,
            "scanify_database"
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    @Singleton
    fun provideDocumentDao(database: DocumentDatabase): DocumentDao {
        return database.documentDao()
    }

    @Provides
    @Singleton
    fun provideExportStorageManager(
        @ApplicationContext context: Context,
        database: DocumentDatabase,
        documentRepository: DocumentRepository
    ): ExportStorageManager {
        return ExportStorageManager(
            context = context,
            database = database,
            dbName = "scanify_database",
            documentRepository = documentRepository
        )
    }

}