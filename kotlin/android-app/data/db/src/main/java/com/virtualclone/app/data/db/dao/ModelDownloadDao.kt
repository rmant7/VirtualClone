package com.virtualclone.app.data.db.dao

import androidx.room.*
import com.virtualclone.app.data.db.entity.ModelDownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDownloadDao {
    @Query("SELECT * FROM model_downloads WHERE modelName = :name LIMIT 1")
    fun observeByNameFlow(name: String): Flow<ModelDownloadEntity?>

    @Query("SELECT * FROM model_downloads")
    fun observeAllFlow(): Flow<List<ModelDownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ModelDownloadEntity)

    @Update
    suspend fun update(entity: ModelDownloadEntity) // this is not being used.

    @Query("DELETE FROM model_downloads WHERE modelName = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT * FROM model_downloads WHERE modelName = :name LIMIT 1")
    suspend fun getByName(name: String): ModelDownloadEntity?

    @Query("SELECT * FROM model_downloads")
    suspend fun getAll(): List<ModelDownloadEntity>
}
