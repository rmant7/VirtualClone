package com.virtualclone.app.di

import android.content.Context
import com.virtualclone.app.core.domain.di.IoDispatcher
import com.virtualclone.app.data.db.VirtualCloneDatabase
import com.virtualclone.app.data.modeldownload.datasource.ResumableDownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {
    @Provides
    @Singleton
    fun provideResumableDownloadManager(
        @ApplicationContext context: Context,
        db: VirtualCloneDatabase,
        client: OkHttpClient,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ResumableDownloadManager {
        return ResumableDownloadManager(context, db, client, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideModelDownloadDao(db: VirtualCloneDatabase) =
        db.modelDownloadDao()
}