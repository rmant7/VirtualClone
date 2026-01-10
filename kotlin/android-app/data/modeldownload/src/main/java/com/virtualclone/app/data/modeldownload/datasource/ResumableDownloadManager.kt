package com.virtualclone.app.data.modeldownload.datasource

import android.content.Context
import android.util.Log
import com.virtualclone.app.core.domain.di.IoDispatcher
import com.virtualclone.app.data.db.VirtualCloneDatabase
import com.virtualclone.app.data.db.entity.ModelDownloadEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResumableDownloadManager @Inject constructor(
    private val context: Context,
    database: VirtualCloneDatabase,
    private val client: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val dao = database.modelDownloadDao()

    /**
     * Keeps track of active download jobs in memory.
     * This is the single source of truth for "isDownloading".
     */
    private val downloads = ConcurrentHashMap<String, DownloadJob>()

    private val tag = ResumableDownloadManager::class.qualifiedName

    /**
     * Progress updates emitted to UI + foreground service.
     *
     * speedBytesPerSec is calculated locally (not persisted),
     * because speed is a transient runtime metric.
     */
    data class Progress(
        val modelName: String,
        val downloaded: Long,
        val total: Long,
        val status: String,
        val speedBytesPerSec: Long = 0L,
        val error: String? = null
    )

    private val _progress = MutableSharedFlow<Progress>(replay = 1)
    val progress: SharedFlow<Progress> = _progress.asSharedFlow()

    /**
     * Dedicated IO scope for all download work.
     * SupervisorJob ensures one failing download does NOT cancel others.
     */
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    fun observeProgress(): SharedFlow<Progress> = progress

    // Exposed so Service or callers can check whether downloads are running
    fun hasActiveDownloads(): Boolean = downloads.isNotEmpty()

    /**
     * Starts a new download or resumes a previously paused one.
     *
     * This function is idempotent:
     * calling it again while a download is active does nothing.
     */
    suspend fun startOrResume(
        modelName: String,
        url: String,
        fileName: String,
        needsAuth: Boolean,
        authToken: String?
    ) {
        Log.i(tag, "Preparing to start or resume download for model: $modelName")

        // Load previous state from DB (if any)
        val existing = dao.getByName(modelName)
        val downloadedBytes = existing?.downloadedBytes ?: 0L
        val totalBytes = existing?.totalBytes ?: -1L

        // Persist DOWNLOADING state immediately (app may be killed anytime)
        dao.upsert(
            ModelDownloadEntity(
                modelName = modelName,
                url = url,
                fileName = fileName,
                totalBytes = totalBytes,
                downloadedBytes = downloadedBytes,
                status = "DOWNLOADING"
            )
        )

        // Prevent duplicate parallel downloads of same model
        if (downloads.containsKey(modelName)) {
            Log.w(tag, "Download for $modelName already in progress. Ignoring duplicate start.")
            return
        }

        val job = scope.launch {
            Log.i(tag, "Starting or resuming download: $modelName ($url)")

            var currentDownloaded = downloadedBytes
            var lastDbPersistTime = 0L
            var lastSpeedCalcTime = System.currentTimeMillis()
            var bytesSinceLastSpeedCalc = 0L

            try {
                val file = File(context.filesDir, fileName)
                if (!file.exists()) {
                    file.createNewFile()
                    Log.i(tag, "Created new file for download: ${file.absolutePath}")
                }

                RandomAccessFile(file, "rw").use { raf ->
                    raf.seek(currentDownloaded)

                    val requestBuilder = Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "VirtualClone-Android")

                    if (!authToken.isNullOrBlank() && needsAuth) {
                        requestBuilder.addHeader("Authorization", "Bearer $authToken")
                    }

                    if (currentDownloaded > 0L) {
                        requestBuilder.header("Range", "bytes=$currentDownloaded-")
                        Log.d(tag, "Resuming from byte: $currentDownloaded")
                    }

                    val request = requestBuilder.build()

                    client.newCall(request).execute().use { response ->
                        if (response.code !in listOf(200, 206)) {
                            throw Exception("HTTP ${response.code}")
                        }

                        val body = response.body
                            ?: throw Exception("Response body is null for $modelName")

                        val contentLength = body.contentLength()
                        val total =
                            if (totalBytes == -1L && contentLength > 0)
                                currentDownloaded + contentLength
                            else totalBytes

                        // Persist known total size
                        dao.upsert(
                            ModelDownloadEntity(
                                modelName,
                                url,
                                fileName,
                                total,
                                currentDownloaded,
                                "DOWNLOADING"
                            )
                        )

                        val source = body.source()
                        val buffer = ByteArray(8 * 1024)

                        while (isActive) {
                            val read = source.read(buffer)
                            if (read == -1) break

                            raf.write(buffer, 0, read)
                            currentDownloaded += read
                            bytesSinceLastSpeedCalc += read

                            val now = System.currentTimeMillis()

                            val speed =
                                if (now - lastSpeedCalcTime >= 1000) {
                                    val elapsed = now - lastSpeedCalcTime
                                    val calculatedSpeed =
                                        (bytesSinceLastSpeedCalc * 1000) / elapsed
                                    bytesSinceLastSpeedCalc = 0
                                    lastSpeedCalcTime = now
                                    calculatedSpeed
                                } else 0L

                            if (now - lastDbPersistTime >= 500) {
                                lastDbPersistTime = now

                                dao.upsert(
                                    ModelDownloadEntity(
                                        modelName,
                                        url,
                                        fileName,
                                        total,
                                        currentDownloaded,
                                        "DOWNLOADING"
                                    )
                                )

                                _progress.emit(
                                    Progress(
                                        modelName = modelName,
                                        downloaded = currentDownloaded,
                                        total = total,
                                        status = "DOWNLOADING",
                                        speedBytesPerSec = speed
                                    )
                                )
                            }
                        }
                    }
                }

                // ----------------------------
                // Download completed
                // ----------------------------
                val finalSize = File(context.filesDir, fileName).length()

                dao.upsert(
                    ModelDownloadEntity(
                        modelName,
                        url,
                        fileName,
                        finalSize,
                        finalSize,
                        "COMPLETED"
                    )
                )

                _progress.emit(
                    Progress(
                        modelName = modelName,
                        downloaded = finalSize,
                        total = finalSize,
                        status = "COMPLETED"
                    )
                )

                Log.i(
                    tag,
                    "Download completed successfully for $modelName. Size: $finalSize bytes"
                )

            } catch (e: CancellationException) {
                Log.i(tag, "Download paused for $modelName at $currentDownloaded bytes")

                dao.upsert(
                    ModelDownloadEntity(
                        modelName = modelName,
                        url = url,
                        fileName = fileName,
                        totalBytes = totalBytes,
                        downloadedBytes = currentDownloaded,
                        status = "PAUSED"
                    )
                )

                _progress.emit(
                    Progress(
                        modelName = modelName,
                        downloaded = currentDownloaded,
                        total = totalBytes,
                        status = "PAUSED"
                    )
                )

            } catch (e: Exception) {
                Log.e(tag, "Download failed for $modelName", e)

                dao.upsert(
                    ModelDownloadEntity(
                        modelName = modelName,
                        url = url,
                        fileName = fileName,
                        totalBytes = totalBytes,
                        downloadedBytes = currentDownloaded,
                        status = "FAILED"
                    )
                )

                _progress.emit(
                    Progress(
                        modelName = modelName,
                        downloaded = currentDownloaded,
                        total = totalBytes,
                        status = "FAILED",
                        error = e.localizedMessage
                    )
                )
            } finally {
                downloads.remove(modelName)
            }
        }

        downloads[modelName] = DownloadJob(job)
    }

    /**
     * Pauses an active download.
     * State is persisted so resume works after app restart.
     */
    fun pause(modelName: String) {
        Log.i(tag, "Pause requested for $modelName")
        downloads.remove(modelName)?.job?.cancel()

        scope.launch {
            val entity = dao.getByName(modelName)
            dao.upsert(
                entity?.copy(status = "PAUSED")
                    ?: ModelDownloadEntity(
                        modelName,
                        "",
                        "",
                        -1L,
                        0L,
                        "PAUSED"
                    )
            )

            _progress.emit(
                Progress(
                    modelName,
                    entity?.downloadedBytes ?: 0L,
                    entity?.totalBytes ?: -1L,
                    "PAUSED"
                )
            )

            Log.d(tag, "Paused $modelName. Progress saved to database.")
        }
    }

    /**
     * Cancels a download completely and removes all traces.
     */
    fun cancel(modelName: String) {
        Log.i(tag, "Cancel requested for $modelName")
        downloads[modelName]?.job?.cancel()

        scope.launch {
            val entity = dao.getByName(modelName)

            // Delete DB entry
            dao.deleteByName(modelName)

            // Delete file from disk
            entity?.fileName?.let { fileName ->
                val file = File(context.filesDir, fileName)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.w(tag, "Deleted model file $fileName: success=$deleted")
                }
            }

            _progress.emit(
                Progress(
                    modelName,
                    0L,
                    -1L,
                    "CANCELED"
                )
            )

            downloads.remove(modelName)
            Log.i(tag, "Canceled and removed $modelName from active list.")
        }
    }

    fun fetchRemoteFileSize(
        url: String,
        authToken: String?
    ): Long {
        val requestBuilder = Request.Builder()
            .url(url)
            .head()
            .addHeader("User-Agent", "VirtualClone-Android")

        if (!authToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $authToken")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HEAD request failed: HTTP ${response.code}")
            }

            val length = response.header("Content-Length")?.toLongOrNull()
            if (length != null && length > 0) {
                return length
            }
        }

        // Fallback: Range request (some servers block HEAD)
        val rangeRequest = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=0-0")
            .apply {
                if (!authToken.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $authToken")
                }
            }
            .build()

        client.newCall(rangeRequest).execute().use { response ->
            val contentRange = response.header("Content-Range")
                ?: throw Exception("Missing Content-Range header")

            // Example: bytes 0-0/12345678
            val total = contentRange.substringAfter("/")
                .toLongOrNull()
                ?: throw Exception("Invalid Content-Range: $contentRange")

            return total
        }
    }


    fun isDownloading(modelId: String): Boolean {
        return downloads.containsKey(modelId)
    }

    private data class DownloadJob(val job: Job)
}
