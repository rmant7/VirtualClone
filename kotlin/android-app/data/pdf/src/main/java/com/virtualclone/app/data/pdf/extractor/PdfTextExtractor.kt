package com.virtualclone.app.data.pdf.extractor

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class PdfTextExtractor(private val context: Context) {
    private val tag = PdfTextExtractor::class.qualifiedName

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun extractText(uri: Uri): String = withContext(Dispatchers.IO) {
        Log.d(tag, "extractText() called for: $uri")
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")

            val document = PDDocument.load(inputStream)
            val textStripper = PDFTextStripper()

            val text = textStripper.getText(document)
            Log.d(tag, "Raw extracted text length = ${text.length}")
            if (text.length > 500) {
                Log.d(tag, "Preview (first 500 chars): ${text.take(500)}")
            } else {
                Log.d(tag, "Full text: $text")
            }

            document.close()
            inputStream.close()

            Log.d(tag, "Extracted text length: $text")
            text
        } catch (e: Exception) {
            Log.e(tag, "Error extracting text from PDF", e)
            throw e
        }
    }

    suspend fun getPageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        Log.d(tag, "getPageCount() called for: $uri")
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")

            val document = PDDocument.load(inputStream)
            val pageCount = document.numberOfPages
            Log.d(tag, "Page count = $pageCount")
            document.close()
            inputStream.close()

            pageCount
        } catch (e: Exception) {
            Log.e(tag, "Error getting page count from PDF", e)
            throw e
        }
    }
}
