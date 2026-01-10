package com.virtualclone.app.data.pdf.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.domain.di.IoDispatcher
import com.virtualclone.app.data.db.VirtualCloneDatabase
import com.virtualclone.app.data.db.entity.ChunkEntity
import com.virtualclone.app.data.db.entity.DocumentEntity
import com.virtualclone.app.data.pdf.chunker.TextChunker
import com.virtualclone.app.data.pdf.extractor.PdfTextExtractor
import com.virtualclone.app.data.pdf.model.PdfDocument
import com.virtualclone.app.data.pdf.model.TextChunk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class DocumentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: VirtualCloneDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
){
    private val tag = DocumentRepository::class.qualifiedName

    private val pdfExtractor = PdfTextExtractor(context)
    private val textChunker = TextChunker()

    fun getAllDocuments(): Flow<List<PdfDocument>> {
        return database.documentDao().getAllDocuments().map { entities ->
            entities.map { entity ->
                PdfDocument(
                    id = entity.id,
                    name = entity.name,
                    uri = entity.uri,
                    size = entity.size,
                    pageCount = entity.pageCount,
                    importedAt = entity.importedAt
                )
            }
        }
    }

    suspend fun getDocumentById(id: String): PdfDocument? {
        val entity = database.documentDao().getDocumentById(id)
        return entity?.let {
            PdfDocument(
                id = it.id,
                name = it.name,
                uri = it.uri,
                size = it.size,
                pageCount = it.pageCount,
                importedAt = it.importedAt
            )
        }
    }

    suspend fun importPdf(uri: Uri, name: String): Result<PdfDocument> =
        withContext(ioDispatcher) {
            Log.d(tag, "==== Import start for $name ====")
            try {
                val text = pdfExtractor.extractText(uri)
                val pageCount = pdfExtractor.getPageCount(uri)

                // Get file size
                val size = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.available().toLong()
                } ?: 0L
                Log.d(tag, "File stats — size: $size bytes, pages: $pageCount")

                // Create document entity
                val documentId = UUID.randomUUID().toString()
                val document = DocumentEntity(
                    id = documentId,
                    name = name,
                    uri = uri.toString(),
                    size = size,
                    pageCount = pageCount,
                    importedAt = Date()
                )

                // Save document to database
                database.documentDao().insertDocument(document)
                Log.d(tag, "Inserted DocumentEntity → id=$documentId, name=$name")

                // Chunk the text
                val chunks = textChunker.chunkText(text, documentId)

                // Save chunks to database
                val chunkEntities = chunks.map { chunk ->
                    ChunkEntity(
                        uuid = chunk.id,
                        docId = chunk.docId,
                        idx = chunk.idx,
                        text = chunk.text
                    )
                }
                database.chunkDao().insertChunks(chunkEntities)
                Log.d(tag, "Inserted ${chunkEntities.size} ChunkEntities into DB")

                chunkEntities.forEach {
                    Log.d(tag, "Chunk idx=${it.idx}, id=${it.uuid}, textPreview=${it.text.take(100)}")
                }

                Log.d(tag, "==== Import complete for $name ====")

                Result.Success(
                    PdfDocument(
                        id = document.id,
                        name = document.name,
                        uri = document.uri,
                        size = document.size,
                        pageCount = document.pageCount,
                        importedAt = document.importedAt,
                        chunks = chunks.map { chunk ->
                            TextChunk(
                                id = chunk.id,
                                docId = chunk.docId,
                                idx = chunk.idx,
                                text = chunk.text,
                                pageNumber = chunk.pageNumber
                            )
                        }
                    )
                )
            } catch (e: Exception) {
                Log.e(tag, "Error importing PDF: $name", e)
                Result.Error(e)
            }
        }

    suspend fun deleteDocument(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Delete chunks first (foreign key constraint)
            database.chunkDao().deleteChunksByDocument(id)

            // Delete document
            database.documentDao().deleteDocumentById(id)

            Log.d(tag, "Successfully deleted document: $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting document: $id", e)
            Result.Error(e)
        }
    }

    suspend fun searchChunks(query: String, limit: Int = 5): Result<List<ChunkEntity>> =
        withContext(ioDispatcher) {
            try {
                val chunks = database.chunkDao().searchChunks(query, limit)
                Result.Success(chunks)
            } catch (e: Exception) {
                Log.e(tag, "Error searching chunks", e)
                Result.Error(e)
            }
        }
}
