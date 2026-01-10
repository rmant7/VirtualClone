package com.virtualclone.app.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.virtualclone.app.data.db.dao.ChunkDao
import com.virtualclone.app.data.db.dao.DocumentDao
import com.virtualclone.app.data.db.dao.MessageDao
import com.virtualclone.app.data.db.entity.ChunkEntity
import com.virtualclone.app.data.db.entity.DocumentEntity
import com.virtualclone.app.data.db.entity.MessageEntity
import com.virtualclone.app.data.db.converter.DateConverter
import com.virtualclone.app.data.db.dao.ModelDownloadDao
import com.virtualclone.app.data.db.entity.ChunkFtsEntity
import com.virtualclone.app.data.db.entity.ModelDownloadEntity

@Database(
    entities = [
        MessageEntity::class,
        DocumentEntity::class,
        ChunkFtsEntity::class,
        ChunkEntity::class,
        ModelDownloadEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class VirtualCloneDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun documentDao(): DocumentDao
    abstract fun chunkDao(): ChunkDao
    abstract fun modelDownloadDao(): ModelDownloadDao

    companion object {
        @Volatile
        private var INSTANCE: VirtualCloneDatabase? = null
        
        fun getDatabase(context: Context): VirtualCloneDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VirtualCloneDatabase::class.java,
                    "virtualclone_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
