package com.dahua.nmea

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dahua.nmea.utils.FileManager
import com.dahua.nmea.utils.NmeaGenerator
import com.dahua.nmea.utils.PassiveGpsListener
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Background service that monitors DSJ Camera recordings and automatically
 * creates matching NMEA tracking files with synchronized timestamps
 */
class DsjCameraMonitorService : Service() {
    
    companion object {
        private const val TAG = "DsjCameraMonitor"
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "dsj_monitor_channel"
        private const val DSJ_VIDEO_PATH = "/storage/emulated/0/mpt"
        private const val POLL_INTERVAL_MS = 500L // Check every 0.5 seconds for faster detection
    }
    
    private var fileObserver: FileObserver? = null
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var passiveGpsListener: PassiveGpsListener? = null
    private var currentNmeaFile: File? = null
    private var currentNmeaWriter: FileWriter? = null
    private var currentVideoFile: File? = null
    private var lastVideoSize: Long = 0
    private var recordingStartTime: Long = 0
    private var lastCheckedVideoPath: String? = null
    private var lastProcessedVideos = mutableSetOf<String>() // Track processed videos
    private val gpsBuffer = mutableListOf<Pair<Long, Location>>() // Buffer of (timestamp, location)
    private val maxBufferSize = 300 // Keep last 5 minutes (300 seconds)
    
    override fun onCreate() {
        super.onCreate()
Log.e(TAG, "=== SERVICE CREATED (PASSIVE GPS MODE) ===")

        try {
            startForeground(NOTIFICATION_ID, createNotification())
            startPassiveGpsListening() // Start passive GPS listening immediately
            
            // Add test GPS data for verification (will be replaced by real GPS)
            addTestGpsData()
            
            startMonitoring()
            Log.e(TAG, "=== SERVICE STARTED SUCCESSFULLY - LISTENING TO DSJ CAMERA GPS ===")
        } catch (e: Exception) {
            Log.e(TAG, "=== SERVICE CREATE ERROR ===", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startMonitoring() {
        try {
            val dsjPath = File(DSJ_VIDEO_PATH)
            Log.e(TAG, "Checking DSJ path: ${dsjPath.absolutePath}")
            Log.e(TAG, "DSJ path exists: ${dsjPath.exists()}")
            
            if (!dsjPath.exists()) {
                Log.e(TAG, "DSJ path not found: $DSJ_VIDEO_PATH")
                return
            }
            
            Log.e(TAG, "Starting polling-based monitor on: $DSJ_VIDEO_PATH")
            
            // Use polling approach - more reliable for deep directory structures
            pollingJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    try {
                        checkForActiveRecording()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in polling", e)
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }
            
            Log.e(TAG, "Monitor started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting monitor", e)
        }
    }
    
    private fun checkForActiveRecording() {
        val dsjPath = File(DSJ_VIDEO_PATH)
        
        // Find the most recent MP4 file
        val recentVideo = dsjPath.walkTopDown()
            .filter { it.isFile && it.extension == "mp4" }
            .maxByOrNull { it.lastModified() }
        
        if (recentVideo == null) {
            Log.e(TAG, "No videos found in DSJ folder")
            if (currentVideoFile != null) {
                onVideoRecordingStopped()
            }
            return
        }
        
        val videoPath = recentVideo.absolutePath
        val currentSize = recentVideo.length()
        val ageSeconds = (System.currentTimeMillis() - recentVideo.lastModified()) / 1000
        
        Log.e(TAG, "Recent video: ${recentVideo.name}, size: $currentSize, age: ${ageSeconds}s")
        
        // Check if this is a NEW video we haven't seen before
        if (!lastProcessedVideos.contains(videoPath)) {
            // New video detected
            if (ageSeconds < 5) { // Created within last 5 seconds
                Log.e(TAG, "NEW VIDEO DETECTED! Creating NMEA retroactively")
                // Create NMEA file for this video (retroactive)
                createNmeaForCompletedVideo(recentVideo)
                lastProcessedVideos.add(videoPath)
            } else {
                // Old video, just mark as processed
                Log.e(TAG, "Old video, marking as processed")
                lastProcessedVideos.add(videoPath)
            }
            return
        }
        
        // Existing video - check if growing
        if (currentVideoFile == null) {
            Log.e(TAG, "No active tracking, checking if file is growing...")
            if (isFileGrowing(recentVideo)) {
                Log.e(TAG, "File is growing! Starting tracking")
                onVideoRecordingStarted(recentVideo)
            } else {
                Log.e(TAG, "File not growing (completed recording)")
            }
        } else if (currentVideoFile!!.absolutePath == recentVideo.absolutePath) {
            if (currentSize > lastVideoSize) {
                lastVideoSize = currentSize
                Log.e(TAG, "Recording continues: ${recentVideo.name}, size: $currentSize")
            } else {
                Log.e(TAG, "Recording stopped (size stable): ${recentVideo.name}")
                onVideoRecordingStopped()
            }
        } else {
            Log.e(TAG, "Different file detected, stopping previous tracking")
            onVideoRecordingStopped()
            if (isFileGrowing(recentVideo)) {
                onVideoRecordingStarted(recentVideo)
            }
        }
        
        // Clean up old processed videos (keep last 50)
        if (lastProcessedVideos.size > 50) {
            lastProcessedVideos.clear()
            lastProcessedVideos.add(videoPath)
        }
    }
    
    private fun createNmeaForCompletedVideo(videoFile: File) {
        try {
            val timestamp = extractTimestampFromDsjVideo(videoFile)
            val nmeaDir = FileManager.getNmeaDirectory(this)
            if (!nmeaDir.exists()) {
                nmeaDir.mkdirs()
            }
            val nmeaFile = File(nmeaDir.absolutePath, "NMEA_${timestamp}.txt")
            
            Log.e(TAG, "Creating NMEA retroactively: ${nmeaFile.name}")
            
            // Calculate video recording time window
            val videoModified = videoFile.lastModified()
            val videoDuration = extractDurationFromFilename(videoFile)
            val videoStartTime = videoModified - (videoDuration * 1000) // Start time
            val videoEndTime = videoModified
            
            Log.e(TAG, "Video time window: ${Date(videoStartTime)} to ${Date(videoEndTime)}")
            
            // Get GPS data from buffer for this time window
            val relevantGpsData = synchronized(gpsBuffer) {
                gpsBuffer.filter { (timestamp, _) ->
                    timestamp >= videoStartTime && timestamp <= videoEndTime
                }.toList()
            }
            
            val writer = FileWriter(nmeaFile, false)
            writer.write("# NMEA GPS Tracking for DSJ Camera (Retroactive)\n")
            writer.write("# Video: ${videoFile.name}\n")
            writer.write("# Created: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            writer.write("# GPS Points: ${relevantGpsData.size}\n")
            writer.write("# Duration: ${videoDuration}s\n")
            
            if (relevantGpsData.isNotEmpty()) {
                Log.e(TAG, "Writing ${relevantGpsData.size} GPS points from buffer")
                relevantGpsData.forEach { (_, location) ->
                    val nmeaSentence = NmeaGenerator.generateGGA(location)
                    writer.write("$nmeaSentence\n")
                }
                Log.e(TAG, "NMEA file with GPS data created: ${nmeaFile.absolutePath}")
            } else {
                writer.write("# Note: No GPS data available in buffer for this time window\n")
                Log.e(TAG, "NMEA placeholder created (no GPS data): ${nmeaFile.absolutePath}")
            }
            
            writer.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating retroactive NMEA", e)
        }
    }
    
    private fun isFileGrowing(file: File): Boolean {
        val size1 = file.length()
        if (size1 < 10240) { // Less than 10KB, too small
            Log.e(TAG, "File too small (${size1} bytes), waiting...")
            return false
        }
        
        Thread.sleep(500) // Wait 0.5 second
        val size2 = file.length()
        
        val isGrowing = size2 > size1
        Log.e(TAG, "File growth check: $size1 -> $size2, growing: $isGrowing")
        return isGrowing
    }
    
    private fun onVideoRecordingStarted(videoFile: File) {
        try {
            Log.d(TAG, "=== RECORDING STARTED ===")
            Log.d(TAG, "Video file: ${videoFile.absolutePath}")
            
            // Extract timestamp from DSJ video filename
            val timestamp = extractTimestampFromDsjVideo(videoFile)
            Log.d(TAG, "Extracted timestamp: $timestamp")
            
            currentVideoFile = videoFile
            lastVideoSize = videoFile.length()
            recordingStartTime = System.currentTimeMillis()
            
            // Create NMEA file with matching timestamp
            val nmeaDir = FileManager.getNmeaDirectory(this)
            currentNmeaFile = File(nmeaDir.absolutePath, "NMEA_${timestamp}.txt")
            currentNmeaWriter = FileWriter(currentNmeaFile, true)
            
            Log.d(TAG, "NMEA file: ${currentNmeaFile?.absolutePath}")
            
            // Write header
            currentNmeaWriter?.write("# NMEA GPS Tracking for DSJ Camera\n")
            currentNmeaWriter?.write("# Video: ${videoFile.name}\n")
            currentNmeaWriter?.write("# Started: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            currentNmeaWriter?.flush()

            Log.d(TAG, "Passive GPS listening active (piggybacking on DSJ Camera)")
            updateNotification("Tracking: ${videoFile.name}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting tracking: ${e.message}", e)
        }
    }
    
    private fun onVideoRecordingStopped() {
        if (currentVideoFile != null) {
            Log.d(TAG, "=== RECORDING STOPPED ===")
            Log.d(TAG, "Video: ${currentVideoFile?.name}")

            currentNmeaWriter?.write("# Stopped:${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            currentNmeaWriter?.close()
            currentNmeaWriter = null
            
            Log.d(TAG, "NMEA file saved: ${currentNmeaFile?.absolutePath}")
            
            currentNmeaFile = null
            currentVideoFile = null
            lastVideoSize = 0
            
            updateNotification("Monitoring DSJ Camera (Passive GPS active)...")
        }
    }
    
    private fun extractDurationFromFilename(videoFile: File): Long {
        // Extract duration from DSJ filename format: HH.MM.SS-HH.MM.SS
        val pattern = "(\\d{2})\\.(\\d{2})\\.(\\d{2})-(\\d{2})\\.(\\d{2})\\.(\\d{2})".toRegex()
        val match = pattern.find(videoFile.name)
        
        return if (match != null) {
            val (h1, m1, s1, h2, m2, s2) = match.destructured
            val startSeconds = h1.toInt() * 3600 + m1.toInt() * 60 + s1.toInt()
            val endSeconds = h2.toInt() * 3600 + m2.toInt() * 60 + s2.toInt()
            (endSeconds - startSeconds).toLong()
        } else {
            30L // Default 30 seconds if can't parse
        }
    }
    
    private fun extractTimestampFromDsjVideo(videoFile: File): String {
        // DSJ format: HH.MM.SS-HH.MM.SS[R][0@0][0].mp4
        // We want format: yyyyMMdd_HHmmss
        
        val fileName = videoFile.name
        
        // Get date from parent folder structure: /mpt/YYYY-MM-DD/...
        val pathParts = videoFile.absolutePath.split("/")
        val dateFolder = pathParts.find { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) }
        val dateStr = dateFolder?.replace("-", "") ?: run {
            // Fallback to file's last modified date
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            sdf.format(Date(videoFile.lastModified()))
        }
        
        // Extract start time from filename (before the dash)
        val timePattern = Regex("""(\d{2})\.(\d{2})\.(\d{2})""")
        val match = timePattern.find(fileName)
        
        return if (match != null) {
            val (hh, mm, ss) = match.destructured
            "${dateStr}_${hh}${mm}${ss}"
        } else {
            // Fallback to current time
            val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            timeFormat.format(Date(videoFile.lastModified()))
        }
    }
    
private fun startPassiveGpsListening() {
        try {
            Log.e(TAG, "🚀 Starting PASSIVE GPS listening (piggybacking on DSJ Camera)...")
            
            passiveGpsListener = PassiveGpsListener(this) { location ->
                // Handle GPS updates
                Log.e(TAG, "🌍 PASSIVE GPS UPDATE: lat=${location.latitude}, lon=${location.longitude}, provider=${location.provider}, accuracy=${location.accuracy}m")
                
                // Store location in buffer with timestamp
                synchronized(gpsBuffer) {
                    gpsBuffer.add(Pair(System.currentTimeMillis(), location))
                    Log.e(TAG, "📊 Buffer size: ${gpsBuffer.size}")
                    // Keep buffer size manageable
                    while (gpsBuffer.size > maxBufferSize) {
                        gpsBuffer.removeAt(0)
                    }
                }
                // Also write to current file if recording
                writeNmeaData(location)
            }
            
            passiveGpsListener?.startListening()
            
            Log.e(TAG, "✅ Passive GPS listener active - no battery drain, using DSJ Camera GPS!")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ No location permission: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Passive GPS error", e)
        }
    }

    private fun stopPassiveGpsListening() {
        try {
            passiveGpsListener?.stopListening()
            passiveGpsListener = null
            Log.d(TAG, "Passive GPS listener stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping passive GPS: ${e.message}")
        }
    }
    
    private fun writeNmeaData(location: Location) {
        try {
            currentNmeaWriter?.let { writer ->
                val nmeaSentence = NmeaGenerator.generateGGA(location)
                writer.write("$nmeaSentence\n")
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing NMEA: ${e.message}")
        }
    }
    
    private fun addTestGpsData() {
        Log.e(TAG, "Adding test GPS data for verification...")
        val now = System.currentTimeMillis()
        
        // Simulate GPS points for last 60 seconds (1 point per second)
        for (i in 60 downTo 1) {
            val location = Location("test_provider").apply {
                latitude = -6.198148 + (i * 0.0001) // Simulate movement
                longitude = 106.794385 + (i * 0.0001)
                accuracy = 10f
                altitude = 50.0
                time = now - (i * 1000) // Go back i seconds
            }
            synchronized(gpsBuffer) {
                gpsBuffer.add(Pair(now - (i * 1000), location))
            }
        }
        
        synchronized(gpsBuffer) {
            Log.e(TAG, "Test GPS data added! Buffer size: ${gpsBuffer.size} points")
        }
    }
    
    private fun createNotification(): Notification {
        createNotificationChannel()
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DSJ Camera Monitor (Passive GPS)")
            .setContentText("Listening to DSJ Camera GPS - No battery drain")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DSJ Camera Monitor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DSJ Camera Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors DSJ Camera recordings for GPS tracking"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        fileObserver?.stopWatching()
        stopPassiveGpsListening()
        currentNmeaWriter?.close()
        Log.d(TAG, "Service destroyed (Passive GPS stopped)")
    }
}

