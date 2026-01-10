package com.virtualclone.app.data.modeldownload.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.virtualclone.app.data.modeldownload.datasource.ResumableDownloadManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

private const val CHANNEL_ID = "model_download_channel"
private const val NOTIF_ID = 14321

@AndroidEntryPoint
class ModelDownloadService : Service() {

    @Inject
    lateinit var downloadManager: ResumableDownloadManager

    private val tag = ModelDownloadService::class.qualifiedName

    /**
     * Service-level coroutine scope.
     * Cancels automatically when service is destroyed.
     */
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        Log.i(tag, "Service created")

        // Observe download progress continuously
        observeJob = scope.launch {
            downloadManager.observeProgress().collect { progress ->

                Log.d(
                    tag,
                    "Progress: ${progress.modelName} " +
                            "${progress.downloaded}/${progress.total} " +
                            "status=${progress.status} speed=${progress.speedBytesPerSec}"
                )

                // Update notification content dynamically
                updateNotification(progress)

                // Stop service automatically when no downloads remain
                if (!downloadManager.hasActiveDownloads()) {
                    Log.i(tag, "No active downloads. Stopping service.")
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(tag, "onStartCommand - starting foreground notification")
        startForeground(
            NOTIF_ID,
            buildNotification("Preparing downloads…")
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        observeJob?.cancel()
        Log.i(tag, "Service destroyed")
    }

    // ---------------------------------------------------------
    // Notification helpers
    // ---------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Model download foreground service"
            }
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Updates the foreground notification with progress + speed.
     */
    private fun updateNotification(
        progress: ResumableDownloadManager.Progress
    ) {
        val text = when (progress.status) {
            "DOWNLOADING" -> {
                val percent =
                    if (progress.total > 0)
                        ((progress.downloaded * 100f) / progress.total).roundToInt()
                    else 0

                val speedText = formatSpeed(progress.speedBytesPerSec)

                "${progress.modelName} • $percent% • $speedText"
            }

            "PAUSED" -> "${progress.modelName} • Paused"
            "COMPLETED" -> "${progress.modelName} • Completed"
            "FAILED" -> "${progress.modelName} • Failed"
            else -> "${progress.modelName} • ${progress.status}"
        }

        val notification = buildNotification(text)

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, notification)
    }

    /**
     * Builds a base notification instance.
     */
    private fun buildNotification(contentText: String): Notification {
        val title = "VirtualClone — Model download"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    /**
     * Converts bytes/sec into a human-readable speed string.
     */
    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "—"

        val kb = bytesPerSec / 1024f
        val mb = kb / 1024f

        return when {
            mb >= 1 -> String.format("%.2f MB/s", mb)
            kb >= 1 -> String.format("%.0f KB/s", kb)
            else -> "$bytesPerSec B/s"
        }
    }
}