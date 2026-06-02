package com.scanify.app.di

import android.content.Context
import androidx.room.Room
import com.scanify.app.data.local.dao.DocumentDao
import com.scanify.app.data.local.database.DocumentDatabase
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

}