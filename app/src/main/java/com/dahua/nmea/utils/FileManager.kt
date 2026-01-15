package com.dahua.nmea.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileManager {
    
    private const val APP_FOLDER = "DahuaNmea"
    private const val VIDEO_FOLDER = "Videos"
    private const val NMEA_FOLDER = "NMEA"
    
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    
    fun createVideoFile(context: Context): File {
        val timestamp = dateFormat.format(Date())
        val fileName = "VID_$timestamp.mp4"
        
        val directory = getVideoDirectory(context)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        return File(directory, fileName)
    }
    
    fun createNmeaFile(context: Context): File {
        val timestamp = dateFormat.format(Date())
        val fileName = "NMEA_$timestamp.txt"
        
        val directory = getNmeaDirectory(context)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        return File(directory, fileName)
    }
    
    fun getVideoDirectory(context: Context): File {
        return File(getAppDirectory(context), VIDEO_FOLDER)
    }
    
    fun getNmeaDirectory(context: Context): File {
        return File(getAppDirectory(context), NMEA_FOLDER)
    }
    
    private fun getAppDirectory(context: Context): File {
        // Use app-specific directory that doesn't require WRITE_EXTERNAL_STORAGE on Android 10+
        // Return base directory directly without additional subfolder
        return context.getExternalFilesDir(null) ?: context.filesDir
    }
    
    fun getAllVideoFiles(context: Context): List<File> {
        val directory = getVideoDirectory(context)
        return directory.listFiles()?.filter { it.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    fun getAllNmeaFiles(context: Context): List<File> {
        val directory = getNmeaDirectory(context)
        return directory.listFiles()?.filter { it.extension == "nmea" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    fun getFileSize(file: File): String {
        val sizeInBytes = file.length()
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
            else -> "${sizeInBytes / (1024 * 1024)} MB"
        }
    }
    
    fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
