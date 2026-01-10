package com.virtualclone.app.data.pdf.chunker

import android.util.Log
import java.util.UUID

class TextChunker {

    private val tag = TextChunker::class.qualifiedName

    companion object {
        private const val DEFAULT_CHUNK_SIZE = 500
        private const val DEFAULT_OVERLAP = 50
        private const val MIN_CHUNK_SIZE = 100
    }

    fun chunkText(
        text: String,
        docId: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP
    ): List<TextChunk> {
        Log.d(tag, "Starting chunkText() for docId=$docId, text length=${text.length}")
        val cleanedText = cleanText(text)
        val sentences = splitIntoSentences(cleanedText)
        Log.d(tag, "Sentence count after splitting: ${sentences.size}")

        val chunks = mutableListOf<TextChunk>()
        var currentChunk = StringBuilder()
        var chunkIndex = 0
        var pageNumber = 1

        for (sentence in sentences) {
            // Check if adding this sentence would exceed chunk size
            if (currentChunk.length + sentence.length > chunkSize && currentChunk.isNotEmpty()) {
                // Create chunk from current content
                val chunkText = currentChunk.toString().trim()
                if (chunkText.length >= MIN_CHUNK_SIZE) {
                    val chunk = TextChunk(
                        id = UUID.randomUUID().toString(),
                        docId = docId,
                        idx = chunkIndex++,
                        text = chunkText,
                        pageNumber = pageNumber
                    )
                    chunks.add(chunk)
                    Log.d(tag, "Chunk ${chunk.idx} created with ${chunk.text.length} chars. Preview: ${chunk.text.take(120)}")
                }

                // Start new chunk with overlap
                val overlapText = getOverlapText(currentChunk.toString(), overlap)
                currentChunk = StringBuilder(overlapText)
            }

            currentChunk.append(sentence).append(" ")

            // Simple page detection (you might want to improve this)
            if (sentence.contains("\n\n")) {
                pageNumber++
            }
        }

        // Add the last chunk if it has content
        if (currentChunk.isNotEmpty()) {
            val chunkText = currentChunk.toString().trim()
            if (chunkText.length >= MIN_CHUNK_SIZE) {
                val chunk = TextChunk(
                    id = UUID.randomUUID().toString(),
                    docId = docId,
                    idx = chunkIndex,
                    text = chunkText,
                    pageNumber = pageNumber
                )
                chunks.add(chunk)
                Log.d(tag, "Final chunk ${chunk.idx} created with ${chunk.text.length} chars.")
            }
        }

        Log.d(tag, "Total chunks created = ${chunks.size}")
        return chunks
    }

    private fun cleanText(text: String): String {
        val cleaned = text.replace(Regex("\\s+"), " ")
            .replace(Regex("\\n+"), "\n")
            .replace(Regex("\\t+"), " ")
            .trim()
        Log.d(tag, "Cleaned text length = ${cleaned.length}")
        return cleaned
    }

    private fun splitIntoSentences(text: String): List<String> {
        // Simple sentence splitting - you might want to use a more sophisticated approach
        return text.split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }

    private fun getOverlapText(text: String, overlapSize: Int): String {
        if (text.length <= overlapSize) return text
        return text.takeLast(overlapSize)
    }

    data class TextChunk(
        val id: String,
        val docId: String,
        val idx: Int,
        val text: String,
        val pageNumber: Int
    )
}
