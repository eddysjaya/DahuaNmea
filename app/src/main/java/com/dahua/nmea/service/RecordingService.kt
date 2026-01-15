package com.dahua.nmea.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.core.app.ActivityCompat
import com.dahua.nmea.VideoRecorder
import com.dahua.nmea.R
import com.dahua.nmea.utils.FileManager
import com.dahua.nmea.utils.GpsTracker
import com.dahua.nmea.utils.NmeaGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingService : Service() {
    
    companion object {
        private const val TAG = "RecordingService"
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIFICATION_ID = 1
    }
    
    private val binder = LocalBinder()
    private var callback: RecordingCallback? = null
    
    // Camera2 components
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    // Video recorder
    private var videoRecorder: VideoRecorder? = null
    
    // Recording state
    private var isRecording = false
    private var nmeaFile: File? = null
    
    // GPS tracking
    private var gpsTracker: GpsTracker? = null
    private var nmeaGenerator: NmeaGenerator? = null
    
    // Time tracking
    private val handler = Handler(Looper.getMainLooper())
    private var recordingStartTime = 0L
    
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                val seconds = (elapsed / 1000).toInt()
                callback?.onTimeUpdate(seconds)
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
        fun onTimeUpdate(seconds: Int)
        fun onGpsUpdate(lat: Double, lon: Double, accuracy: Float, pointCount: Int)
        fun onError(message: String)
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        startBackgroundThread()
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    fun setCallback(callback: RecordingCallback) {
        this.callback = callback
    }
    
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }
    
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }
    
    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted")
            return
        }
        
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera opened")
                    cameraDevice = camera
                    startRecordingSession()
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected")
                    cameraDevice?.close()
                    cameraDevice = null
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    cameraDevice?.close()
                    cameraDevice = null
                    callback?.onError("Camera error: $error")
                }
            }, backgroundHandler)
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error opening camera", e)
            callback?.onError("Camera access error")
        }
    }
    
    private fun startRecordingSession() {
        try {
            val surface = videoRecorder?.getSurface()
            if (surface == null) {
                Log.e(TAG, "Recorder surface is null")
                return
            }
            
            val captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            captureRequestBuilder.addTarget(surface)
            
            cameraDevice!!.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "Capture session configured")
                        cameraCaptureSession = session
                        
                        try {
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                backgroundHandler
                            )
                            
                            // Start MediaRecorder
                            if (videoRecorder!!.start()) {
                                Log.i(TAG, "MediaRecorder started successfully")
                            }
                            
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Error starting capture session", e)
                        }
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                        callback?.onError("Failed to configure camera session")
                    }
                },
                backgroundHandler
            )
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error creating capture session", e)
        }
    }
    
    fun getRecorderSurface(): Surface? {
        return videoRecorder?.getSurface()
    }
    
    fun startRecording(): Boolean {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }
        
        try {
            val foregroundNotification = createNotification()
            startForeground(NOTIFICATION_ID, foregroundNotification)
            
            // Create output files using FileManager for consistent paths
            nmeaFile = FileManager.createNmeaFile(this)
            val videoDir = FileManager.getVideoDirectory(this)
            
            Log.d(TAG, "Video directory: ${videoDir.absolutePath}")
            Log.d(TAG, "NMEA file: ${nmeaFile?.absolutePath}")
            
            // Initialize NMEA generator
            nmeaGenerator = NmeaGenerator(nmeaFile!!)
            
            // Initialize GPS tracker with callback
            gpsTracker = GpsTracker(this) { location ->
                Log.d(TAG, "GPS location: ${location.latitude}, ${location.longitude}")
                nmeaGenerator?.addLocation(location)
                val pointCount = nmeaGenerator?.getPointCount() ?: 0
                callback?.onGpsUpdate(
                    location.latitude,
                    location.longitude,
                    location.accuracy,
                    pointCount
                )
            }
            
            // Start GPS tracking
            gpsTracker?.startTracking()
            
            // Initialize video recorder
            videoRecorder = VideoRecorder(this, videoDir)
            
            // Prepare and start recording
            if (!videoRecorder!!.prepare()) {
                Log.e(TAG, "Failed to prepare MediaRecorder")
                callback?.onError("Failed to prepare video recorder")
                cleanup()
                return false
            }
            
            // Open camera for recording
            openCamera()
            
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            handler.post(timeUpdateRunnable)
            callback?.onRecordingStarted()
            
            Log.d(TAG, "Recording started successfully with MediaRecorder")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            callback?.onError(e.message ?: "Unknown error")
            cleanup()
            return false
        }
    }
    
    fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            return
        }
        
        try {
            Log.d(TAG, "Stopping recording")
            
            // Close camera session
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            
            // Close camera
            cameraDevice?.close()
            cameraDevice = null
            
            // Stop video recording
            videoRecorder?.stop()
            
            // Log file information
            val videoFile = videoRecorder?.getCurrentVideoFile()
            Log.d(TAG, "Video file: ${videoFile?.absolutePath}, size: ${videoFile?.length() ?: 0} bytes")
            
            // Stop GPS tracking
            gpsTracker?.stopTracking()
            nmeaGenerator?.close()
            
            isRecording = false
            handler.removeCallbacks(timeUpdateRunnable)
            callback?.onRecordingStopped()
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            Log.d(TAG, "Recording stopped successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            callback?.onError(e.message ?: "Unknown error")
        }
    }
    
    fun isCurrentlyRecording(): Boolean = isRecording
    
    private fun cleanup() {
        try {
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            cameraDevice?.close()
            cameraDevice = null
            videoRecorder?.stop()
            gpsTracker?.stopTracking()
            nmeaGenerator?.close()
            
            isRecording = false
            handler.removeCallbacks(timeUpdateRunnable)
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows recording status"
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Recording in progress...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Recording in progress...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        stopBackgroundThread()
        Log.d(TAG, "Service destroyed")
    }
}
