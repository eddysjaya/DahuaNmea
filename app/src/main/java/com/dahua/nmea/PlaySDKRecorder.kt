package com.dahua.nmea

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import android.view.Surface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MediaRecorder-based video recording for MPT230
 */
class VideoRecorder(private val context: Context, private val outputDir: File) {
    
    companion object {
        private const val TAG = "VideoRecorder"
        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_BITRATE = 6_000_000 // 6 Mbps
        private const val VIDEO_FRAME_RATE = 30
        private const val AUDIO_BITRATE = 128_000 // 128 Kbps
        private const val AUDIO_SAMPLE_RATE = 44100
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentVideoFile: File? = null
    
    /**
     * Get Surface for camera preview
     */
    fun getSurface(): Surface? {
        return mediaRecorder?.surface
    }
    
    /**
     * Prepare MediaRecorder
     */
    fun prepare(): Boolean {
        try {
            // Create output directory if needed
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            currentVideoFile = File(outputDir, "VID_${timestamp}.mp4")
            
            Log.d(TAG, "Preparing recorder for: ${currentVideoFile?.absolutePath}")
            
            // Setup MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                // Set audio source
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // Set video source
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                
                // Set output format
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                
                // Set output file
                setOutputFile(currentVideoFile?.absolutePath)
                
                // Set video encoding parameters
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT)
                setVideoEncodingBitRate(VIDEO_BITRATE)
                setVideoFrameRate(VIDEO_FRAME_RATE)
                
                // Set audio encoding parameters
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(AUDIO_BITRATE)
                setAudioSamplingRate(AUDIO_SAMPLE_RATE)
                
                // Prepare recorder
                prepare()
            }
            
            Log.i(TAG, "MediaRecorder prepared successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing MediaRecorder", e)
            release()
            return false
        }
    }
    
    /**
     * Start recording
     */
    fun start(): Boolean {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }
        
        try {
            mediaRecorder?.start()
            isRecording = true
            Log.i(TAG, "Recording started: ${currentVideoFile?.name}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            release()
            return false
        }
    }
    
    /**
     * Stop recording
     */
    fun stop() {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            return
        }
        
        try {
            mediaRecorder?.stop()
            isRecording = false
            
            val fileSize = currentVideoFile?.length() ?: 0
            Log.i(TAG, "Recording stopped. File size: $fileSize bytes")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            release()
        }
    }
    
    /**
     * Release resources
     */
    private fun release() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * Get current video file
     */
    fun getCurrentVideoFile(): File? = currentVideoFile
}
