package com.virtualclone.app.di

import android.content.Context
import com.virtualclone.app.data.db.VirtualCloneDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): VirtualCloneDatabase {
        return VirtualCloneDatabase.getDatabase(context)
    }
}