package com.dahua.nmea.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object UsbTransferHelper {
    
    /**
     * Share files via USB by opening file manager or share dialog
     */
    fun shareFiles(context: Context, videoFile: File, nmeaFile: File) {
        val uris = ArrayList<Uri>()
        
        // Convert files to content URIs
        val videoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            videoFile
        )
        val nmeaUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            nmeaFile
        )
        
        uris.add(videoUri)
        uris.add(nmeaUri)
        
        // Create share intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Transfer files to PC"))
    }
    
    /**
     * Open file location in file manager
     */
    fun openFileLocation(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file.parentFile ?: file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "resource/folder")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: open with file manager
            val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Open file manager"))
        }
    }
    
    /**
     * Get instructions for USB file transfer
     */
    fun getUsbTransferInstructions(): String {
        return """
            USB File Transfer Instructions:
            
            1. Connect device to PC via USB cable
            2. On device: Swipe down notification panel
            3. Tap "USB charging this device"
            4. Select "File Transfer" or "MTP" mode
            5. On PC: Open "This PC" or "My Computer"
            6. Open device name (e.g., "Dahua MPT230")
            7. Navigate to: Internal Storage/Android/data/com.dahua.nmea/files/DahuaNmea
            8. Copy desired files (Videos or NMEA folders) to PC
            
            Alternative: Use the share button to send files via email or cloud storage.
        """.trimIndent()
    }
}
