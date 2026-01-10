package com.virtualclone.app.data.db.dao

import androidx.room.*
import com.virtualclone.app.data.db.entity.ChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {
    @Query("SELECT * FROM chunks WHERE docId = :docId ORDER BY idx ASC")
    fun getChunksByDocument(docId: String): Flow<List<ChunkEntity>>
    
    @Query("SELECT * FROM chunks WHERE docId = :docId ORDER BY idx ASC")
    suspend fun getChunksByDocumentSync(docId: String): List<ChunkEntity>
    
    @Query("SELECT * FROM chunks WHERE docId = :docId AND idx = :idx")
    suspend fun getChunkByDocumentAndIndex(docId: String, idx: Int): ChunkEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: ChunkEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkEntity>)
    
    @Update
    suspend fun updateChunk(chunk: ChunkEntity)
    
    @Delete
    suspend fun deleteChunk(chunk: ChunkEntity)
    
    @Query("DELETE FROM chunks WHERE docId = :docId")
    suspend fun deleteChunksByDocument(docId: String)
    
    @Query("DELETE FROM chunks")
    suspend fun deleteAllChunks()
    
    // FTS search query
    @Query("""
    SELECT chunks.* FROM chunks
    JOIN chunks_fts ON chunks.rowid = chunks_fts.rowid
    WHERE chunks_fts MATCH :query
    LIMIT :limit
""")
    suspend fun searchChunks(query: String, limit: Int = 5): List<ChunkEntity>

}
