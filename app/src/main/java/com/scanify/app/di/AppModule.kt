package com.scanify.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.scanify.app.data.repositoryimpl.FileManagerImpl
import com.scanify.app.data.repositoryimpl.ThemePreferenceRepositoryImpl
import com.scanify.app.data.repositoryimpl.UriResolverImpl
import com.scanify.app.domain.repository.FileManager
import com.scanify.app.domain.repository.ThemePreferenceRepository
import com.scanify.app.domain.repository.UriResolver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(produceFile = {
            context.preferencesDataStoreFile("scanify_preferences")
        })
    }

    @Provides
    @Singleton
    fun provideFileManager(@ApplicationContext context: Context): FileManager =
        FileManagerImpl(context)

    @Provides
    @Singleton
    fun provideUriResolver(@ApplicationContext context: Context): UriResolver =
        UriResolverImpl(context)

}
