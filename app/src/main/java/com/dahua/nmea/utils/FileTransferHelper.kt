package com.dahua.nmea.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Socket
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Helper class for transferring recorded video and NMEA files
 * Supports: USB (via ADB), WiFi (TCP Socket), and ZIP export
 */
class FileTransferHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "FileTransferHelper"
        private const val TRANSFER_BUFFER_SIZE = 8192
        private const val DEFAULT_PORT = 8888
    }
    
    interface TransferCallback {
        fun onTransferStarted(fileName: String)
        fun onTransferProgress(fileName: String, progress: Int)
        fun onTransferComplete(fileName: String)
        fun onTransferError(fileName: String, error: String)
    }
    
    /**
     * Get all recorded files (video + NMEA pairs)
     * Scans both our app's videos and DSJ Camera videos
     */
    fun getRecordedFiles(): List<RecordedSession> {
        val sessions = mutableListOf<RecordedSession>()
        
        val nmeaDir = FileManager.getNmeaDirectory(context)
        
        Log.e(TAG, "=== CHECKING FILES ===")
        Log.e(TAG, "NMEA dir: ${nmeaDir.absolutePath}, exists: ${nmeaDir.exists()}")
        
        // 1. Scan our app's videos
        val videoDir = FileManager.getVideoDirectory(context)
        if (videoDir.exists()) {
            Log.e(TAG, "Scanning app videos: ${videoDir.absolutePath}")
            val videoFiles = videoDir.listFiles { file -> file.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()
            Log.e(TAG, "Found ${videoFiles.size} app video files")
            
            for (videoFile in videoFiles) {
                // Extract timestamp from video filename (VID_20260114_123456.mp4)
                val timestamp = videoFile.nameWithoutExtension.substringAfter("VID_")
                val nmeaFile = if (nmeaDir.exists()) File(nmeaDir, "NMEA_$timestamp.txt") else null
                
                Log.e(TAG, "App Video: ${videoFile.name}, NMEA: ${nmeaFile?.name}, exists: ${nmeaFile?.exists()}")
                
                sessions.add(RecordedSession(
                    videoFile = videoFile,
                    nmeaFile = nmeaFile?.takeIf { it.exists() },
                    timestamp = timestamp,
                    size = videoFile.length() + (nmeaFile?.takeIf { it.exists() }?.length() ?: 0),
                    source = "App"
                ))
            }
        }
        
        // 2. Scan DSJ Camera videos
        val dsjDir = File("/storage/emulated/0/mpt")
        if (dsjDir.exists()) {
            Log.e(TAG, "Scanning DSJ videos: ${dsjDir.absolutePath}")
            val dsjVideoFiles = dsjDir.walkTopDown()
                .filter { it.isFile && it.extension == "mp4" }
                .sortedByDescending { it.lastModified() }
                .toList()
            Log.e(TAG, "Found ${dsjVideoFiles.size} DSJ video files")
            
            for (videoFile in dsjVideoFiles) {
                // DSJ format: HH.MM.SS-HH.MM.SS[R][0@0][0].mp4
                // Extract timestamp from filename
                val timestamp = extractDsjTimestamp(videoFile)
                val nmeaFile = if (nmeaDir.exists()) File(nmeaDir, "NMEA_$timestamp.txt") else null
                
                Log.e(TAG, "DSJ Video: ${videoFile.name}, NMEA: ${nmeaFile?.name}, exists: ${nmeaFile?.exists()}")
                
                sessions.add(RecordedSession(
                    videoFile = videoFile,
                    nmeaFile = nmeaFile?.takeIf { it.exists() },
                    timestamp = timestamp,
                    size = videoFile.length() + (nmeaFile?.takeIf { it.exists() }?.length() ?: 0),
                    source = "DSJ Camera"
                ))
            }
        }
        
        Log.e(TAG, "Total sessions: ${sessions.size}")
        return sessions.sortedByDescending { it.timestamp }
    }
    
    /**
     * Extract timestamp from DSJ video filename
     * Format: HH.MM.SS-HH.MM.SS[R][0@0][0].mp4 -> yyyyMMdd_HHmmss
     */
    private fun extractDsjTimestamp(videoFile: File): String {
        val fileName = videoFile.name
        val timePattern = Regex("""(\d{2})\.(\d{2})\.(\d{2})""")
        val match = timePattern.find(fileName)
        
        // Get date from parent folder structure: /mpt/YYYY-MM-DD/...
        val pathParts = videoFile.absolutePath.split("/")
        val dateFolder = pathParts.find { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) }
        val dateStr = dateFolder?.replace("-", "") ?: run {
            // Fallback to file's last modified date
            val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(videoFile.lastModified()))
        }
        
        return if (match != null) {
            val (hh, mm, ss) = match.destructured
            "${dateStr}_${hh}${mm}${ss}"
        } else {
            // Fallback to current time
            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            sdf.format(java.util.Date(videoFile.lastModified()))
        }
    }
    
    /**
     * Export files to ZIP for USB transfer
     */
    suspend fun exportToZip(
        sessions: List<RecordedSession>,
        outputFile: File,
        callback: TransferCallback? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating ZIP export: ${outputFile.name}")
            callback?.onTransferStarted(outputFile.name)
            
            ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                var processedBytes = 0L
                val totalBytes = sessions.sumOf { it.size }
                
                sessions.forEach { session ->
                    // Add video file
                    addFileToZip(zipOut, session.videoFile, "videos/")
                    processedBytes += session.videoFile.length()
                    callback?.onTransferProgress(outputFile.name, ((processedBytes * 100) / totalBytes).toInt())
                    
                    // Add NMEA file if exists
                    session.nmeaFile?.let { nmeaFile ->
                        addFileToZip(zipOut, nmeaFile, "nmea/")
                        processedBytes += nmeaFile.length()
                        callback?.onTransferProgress(outputFile.name, ((processedBytes * 100) / totalBytes).toInt())
                    }
                }
            }
            
            Log.d(TAG, "ZIP export complete: ${outputFile.absolutePath}")
            callback?.onTransferComplete(outputFile.name)
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "ZIP export failed", e)
            callback?.onTransferError(outputFile.name, e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    /**
     * Transfer file via WiFi TCP socket
     */
    suspend fun transferViaWiFi(
        file: File,
        serverIp: String,
        port: Int = DEFAULT_PORT,
        callback: TransferCallback? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Transferring via WiFi: ${file.name} to $serverIp:$port")
            callback?.onTransferStarted(file.name)
            
            Socket(serverIp, port).use { socket ->
                socket.soTimeout = 30000 // 30 second timeout
                
                socket.getOutputStream().use { output ->
                    val fileSize = file.length()
                    
                    // Send header: filename|size\n
                    val header = "${file.name}|$fileSize\n"
                    val headerBytes = header.toByteArray(Charsets.UTF_8)
                    output.write(headerBytes)
                    output.flush()
                    
                    // Small delay to ensure header is sent separately
                    Thread.sleep(100)
                    
                    // Send file content
                    FileInputStream(file).use { input ->
                        val buffer = ByteArray(TRANSFER_BUFFER_SIZE)
                        var bytesRead: Int
                        var totalBytes = 0L
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                            
                            val progress = ((totalBytes * 100) / fileSize).toInt()
                            callback?.onTransferProgress(file.name, progress)
                        }
                        
                        output.flush()
                    }
                }
            }
            
            Log.d(TAG, "WiFi transfer complete: ${file.name}")
            callback?.onTransferComplete(file.name)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "WiFi transfer failed: ${file.name}", e)
            callback?.onTransferError(file.name, e.message ?: "Connection failed")
            Result.failure(e)
        }
    }
    
    /**
     * Copy files to USB storage (Download folder accessible via USB)
     */
    suspend fun copyToUsbFolder(
        sessions: List<RecordedSession>,
        callback: TransferCallback? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val usbFolder = File(context.getExternalFilesDir(null), "USB_Transfer")
            if (!usbFolder.exists()) {
                usbFolder.mkdirs()
            }
            
            Log.d(TAG, "Copying files to USB folder: ${usbFolder.absolutePath}")
            
            var copiedFiles = 0
            val totalFiles = sessions.size * 2 // video + nmea
            
            sessions.forEach { session ->
                // Copy video
                val videoDestination = File(usbFolder, session.videoFile.name)
                copyFile(session.videoFile, videoDestination)
                copiedFiles++
                callback?.onTransferProgress("USB Copy", (copiedFiles * 100) / totalFiles)
                
                // Copy NMEA
                session.nmeaFile?.let { nmeaFile ->
                    val nmeaDestination = File(usbFolder, nmeaFile.name)
                    copyFile(nmeaFile, nmeaDestination)
                }
                copiedFiles++
                callback?.onTransferProgress("USB Copy", (copiedFiles * 100) / totalFiles)
            }
            
            Log.d(TAG, "USB copy complete: ${usbFolder.absolutePath}")
            callback?.onTransferComplete("USB Copy")
            Result.success(usbFolder)
            
        } catch (e: Exception) {
            Log.e(TAG, "USB copy failed", e)
            callback?.onTransferError("USB Copy", e.message ?: "Copy failed")
            Result.failure(e)
        }
    }
    
    /**
     * Delete transferred files to free up space
     */
    fun deleteSession(session: RecordedSession): Boolean {
        var success = true
        
        if (session.videoFile.exists()) {
            success = session.videoFile.delete() && success
        }
        
        session.nmeaFile?.let { nmeaFile ->
            if (nmeaFile.exists()) {
                success = nmeaFile.delete() && success
            }
        }
        
        return success
    }
    
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, folder: String) {
        FileInputStream(file).use { input ->
            val entry = ZipEntry("$folder${file.name}")
            zipOut.putNextEntry(entry)
            
            val buffer = ByteArray(TRANSFER_BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                zipOut.write(buffer, 0, bytesRead)
            }
            
            zipOut.closeEntry()
        }
    }
    
    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(TRANSFER_BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
    }
    
    data class RecordedSession(
        val videoFile: File,
        val nmeaFile: File?,
        val timestamp: String,
        val size: Long,
        val source: String = "App" // "App" or "DSJ Camera"
    ) {
        fun getFormattedSize(): String {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            return if (mb > 1) {
                String.format("%.2f MB", mb)
            } else {
                String.format("%.2f KB", kb)
            }
        }
        
        fun getFormattedTimestamp(): String {
            // Format: 20260114_123456 -> 2026-01-14 12:34:56
            return try {
                val year = timestamp.substring(0, 4)
                val month = timestamp.substring(4, 6)
                val day = timestamp.substring(6, 8)
                val hour = timestamp.substring(9, 11)
                val minute = timestamp.substring(11, 13)
                val second = timestamp.substring(13, 15)
                "$year-$month-$day $hour:$minute:$second"
            } catch (e: Exception) {
                timestamp
            }
        }
    }
}
