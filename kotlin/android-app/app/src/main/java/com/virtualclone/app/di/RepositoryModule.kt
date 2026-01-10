package com.virtualclone.app.di

import com.virtualclone.app.core.domain.repository.ChatRepository
import com.virtualclone.app.core.domain.repository.HfTokenProvider
import com.virtualclone.app.core.domain.repository.ModelDownloadRepository
import com.virtualclone.app.core.domain.repository.ModelRepository
import com.virtualclone.app.data.chat.repository.ChatRepositoryImpl
import com.virtualclone.app.data.modeldownload.repository.HfTokenProviderImpl
import com.virtualclone.app.data.modeldownload.repository.ModelDownloadRepositoryImpl
import com.virtualclone.app.data.modelinference.repository.ModelInferenceDataRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindModelRepository(
        impl: ModelInferenceDataRepositoryImpl
    ): ModelRepository

    @Binds
    @Singleton
    abstract fun bindModelDownloadRepository(
        impl: ModelDownloadRepositoryImpl
    ): ModelDownloadRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindHfTokenProvider(
        impl: HfTokenProviderImpl
    ): HfTokenProvider


}