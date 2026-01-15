package com.dahua.nmea.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.dahua.nmea.MainActivity
import com.dahua.nmea.R
import com.dahua.nmea.utils.FileManager
import com.dahua.nmea.utils.GpsTracker
import com.dahua.nmea.utils.NmeaGenerator
import java.io.File

@Suppress("DEPRECATION")
class RecordingService : Service() {

    private val binder = LocalBinder()
    private var callback: RecordingCallback? = null
    
    private var camera: Camera? = null
    private var mediaRecorder: MediaRecorder? = null
    private var gpsTracker: GpsTracker? = null
    private var nmeaGenerator: NmeaGenerator? = null
    
    private var isRecording = false
    private var videoFile: File? = null
    private var nmeaFile: File? = null
    private var recordingStartTime = 0L
    
    private val handler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000
                callback?.onTimeUpdate(elapsed)
                handler.postDelayed(this, 1000)
            }
        }
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }
    
    interface RecordingCallback {
        fun onRecordingStarted()
        fun onRecordingStopped()
        fun onError(error: String)
        fun onGpsUpdate(latitude: Double, longitude: Double, accuracy: Float, pointCount: Int)
        fun onTimeUpdate(seconds: Long)
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
    
    fun setCallbacks(callback: RecordingCallback) {
        this.callback = callback
    }
    
    fun startRecording() {
        if (isRecording) return
        
        try {
            // Create files
            videoFile = FileManager.createVideoFile(this)
            nmeaFile = FileManager.createNmeaFile(this)
            
            // Initialize GPS tracker
            gpsTracker = GpsTracker(this) { location ->
                Log.d(TAG, "GPS location received in service: ${location.latitude}, ${location.longitude}")
                nmeaGenerator?.addLocation(location)
                val pointCount = nmeaGenerator?.getPointCount() ?: 0
                Log.d(TAG, "Calling onGpsUpdate callback with $pointCount points")
                callback?.onGpsUpdate(
                    location.latitude,
                    location.longitude,
                    location.accuracy,
                    pointCount
                )
            }
            
            // Initialize NMEA generator
            nmeaGenerator = NmeaGenerator(nmeaFile!!)
            
            // Start GPS tracking
            gpsTracker?.startTracking()
            
            // Start video recording
            startVideoRecording()
            
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            handler.post(timeUpdateRunnable)
            callback?.onRecordingStarted()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            callback?.onError(e.message ?: "Unknown error")
            cleanup()
        }
    }
    
    fun stopRecording() {
        if (!isRecording) return
        
        try {
            // Stop video recording
            stopVideoRecording()
            
            // Stop GPS tracking
            gpsTracker?.stopTracking()
            nmeaGenerator?.close()
            
            isRecording = false
            handler.removeCallbacks(timeUpdateRunnable)
            callback?.onRecordingStopped()
            
            cleanup()
            stopSelf()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            callback?.onError(e.message ?: "Unknown error")
        }
    }
    
    private fun startVideoRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            callback?.onError("Camera permission not granted")
            return
        }
        
        try {
            // Open Camera1 API (deprecated but more compatible)
            camera = Camera.open(0)
            camera?.setDisplayOrientation(90)
            
            // Setup and start MediaRecorder
            setupMediaRecorder()
            mediaRecorder?.start()
            
            Log.d(TAG, "Camera1 recording started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Camera1 recording", e)
            callback?.onError("Camera error: ${e.message}")
            cleanup()
        }
    }
    
    private fun setupMediaRecorder() {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        
        // Unlock and set camera for MediaRecorder
        camera?.unlock()
        
        mediaRecorder?.apply {
            setCamera(camera)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(videoFile?.absolutePath)
            setVideoEncodingBitRate(512000)
            setVideoFrameRate(15)
            setVideoSize(320, 240)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        
        mediaRecorder?.apply {
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64000)
            setAudioSamplingRate(44100)
            
            // Add error and info listeners
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaRecorder Error: what=$what, extra=$extra")
                callback?.onError("Recording error: $what")
            }
            
            setOnInfoListener { _, what, extra ->
                Log.i(TAG, "MediaRecorder Info: what=$what, extra=$extra")
            }
            
            try {
                prepare()
                Log.d(TAG, "MediaRecorder prepared successfully with Camera1 API")
            } catch (e: Exception) {
                Log.e(TAG, "MediaRecorder prepare failed", e)
                throw e
            }
        }
    }
    
    private fun stopVideoRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media recorder", e)
        }
        mediaRecorder?.release()
        mediaRecorder = null
        
        try {
            camera?.reconnect()
            camera?.stopPreview()
            camera?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera", e)
        }
        camera = null
    }
    
    private fun cleanup() {
        gpsTracker?.stopTracking()
        nmeaGenerator?.close()
        
        stopVideoRecording()
        
        gpsTracker = null
        nmeaGenerator = null
        videoFile = null
        nmeaFile = null
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun createNotificationChannel() {    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_gps)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopRecording()
        }
    }
    
    companion object {
        private const val TAG = "RecordingService"
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIFICATION_ID = 1
    }
}
