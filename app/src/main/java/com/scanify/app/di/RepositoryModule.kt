package com.scanify.app.di

import com.scanify.app.data.repositoryimpl.DocumentRepositoryImpl
import com.scanify.app.data.repositoryimpl.ThemePreferenceRepositoryImpl
import com.scanify.app.domain.repository.DocumentRepository
import com.scanify.app.domain.repository.ThemePreferenceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule{
    @Binds
    abstract fun bindThemePreferenceRepository(
        themePreferenceRepositoryImpl: ThemePreferenceRepositoryImpl
    ): ThemePreferenceRepository

    @Binds
    abstract fun bindDocumentRepository(
        documentRepositoryImpl: DocumentRepositoryImpl
    ): DocumentRepository
}