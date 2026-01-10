package com.virtualclone.app.di

import android.content.Context
import com.virtualclone.app.core.domain.repository.NetworkMonitor
import com.virtualclone.app.data.modeldownload.repository.AndroidNetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            // Do NOT timeout large downloads
            .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)

            // Allow max parallelism (even if future multi-download)
            .dispatcher(
                okhttp3.Dispatcher().apply {
                    maxRequests = 8
                    maxRequestsPerHost = 8
                }
            )

            // Reuse connections aggressively
            .connectionPool(
                okhttp3.ConnectionPool(
                    8,
                    5,
                    java.util.concurrent.TimeUnit.MINUTES
                )
            )
            .build()

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor = AndroidNetworkMonitor(context)
}
