package com.dahua.nmea

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dahua.nmea.databinding.ActivityFileTransferBinding
import com.dahua.nmea.utils.FileTransferHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileTransferActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFileTransferBinding
    private lateinit var transferHelper: FileTransferHelper
    private var sessions: List<FileTransferHelper.RecordedSession> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        transferHelper = FileTransferHelper(this)
        
        setupUI()
        loadRecordedFiles()
    }
    
    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        binding.btnExportZip.setOnClickListener {
            exportToZip()
        }
        
        binding.btnCopyUsb.setOnClickListener {
            copyToUsb()
        }
        
        binding.btnTransferWifi.setOnClickListener {
            showWiFiDialog()
        }
        
        binding.btnDeleteAll.setOnClickListener {
            showDeleteConfirmation()
        }
    }
    
    private fun loadRecordedFiles() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                sessions = transferHelper.getRecordedFiles()
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    updateUI()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@FileTransferActivity, "Error loading files: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun updateUI() {
        binding.tvTotalFiles.text = "Total: ${sessions.size} sessions"
        val totalSize = sessions.sumOf { it.size }
        val totalMB = totalSize / (1024.0 * 1024.0)
        binding.tvTotalSize.text = String.format("Size: %.2f MB", totalMB)
        
        // Show/hide buttons based on file count
        val hasFiles = sessions.isNotEmpty()
        binding.btnExportZip.isEnabled = hasFiles
        binding.btnCopyUsb.isEnabled = hasFiles
        binding.btnTransferWifi.isEnabled = hasFiles
        binding.btnDeleteAll.isEnabled = hasFiles
        
        if (sessions.isEmpty()) {
            binding.tvNoFiles.visibility = View.VISIBLE
        } else {
            binding.tvNoFiles.visibility = View.GONE
        }
    }
    
    private fun exportToZip() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgress.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val zipFile = File(getExternalFilesDir(null), "DahuaNmea_Export_$timestamp.zip")
            
            val callback = object : FileTransferHelper.TransferCallback {
                override fun onTransferStarted(fileName: String) {
                    runOnUiThread {
                        binding.tvProgress.text = "Creating ZIP: $fileName"
                    }
                }
                
                override fun onTransferProgress(fileName: String, progress: Int) {
                    runOnUiThread {
                        binding.tvProgress.text = "Creating ZIP: $progress%"
                    }
                }
                
                override fun onTransferComplete(fileName: String) {
                    // Will be handled after all deletions
                }
                
                override fun onTransferError(fileName: String, error: String) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        binding.tvProgress.visibility = View.GONE
                        Toast.makeText(this@FileTransferActivity, "Export failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
            
            val result = transferHelper.exportToZip(sessions, zipFile, callback)
            
            if (result.isSuccess) {
                // Delete all sessions after successful ZIP creation
                var deleted = 0
                sessions.forEach { session ->
                    if (transferHelper.deleteSession(session)) {
                        deleted++
                    }
                }
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    Toast.makeText(this@FileTransferActivity, 
                        "ZIP created! ($deleted sessions deleted)\n${zipFile.absolutePath}", 
                        Toast.LENGTH_LONG).show()
                    loadRecordedFiles()  // Reload to update UI
                }
            }
        }
    }
    
    private fun copyToUsb() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgress.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val callback = object : FileTransferHelper.TransferCallback {
                override fun onTransferStarted(fileName: String) {
                    runOnUiThread {
                        binding.tvProgress.text = "Copying to USB folder..."
                    }
                }
                
                override fun onTransferProgress(fileName: String, progress: Int) {
                    runOnUiThread {
                        binding.tvProgress.text = "Copying: $progress%"
                    }
                }
                
                override fun onTransferComplete(fileName: String) {
                    // Will be handled after all deletions
                }
                
                override fun onTransferError(fileName: String, error: String) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        binding.tvProgress.visibility = View.GONE
                        Toast.makeText(this@FileTransferActivity, "Copy failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
            
            val result = transferHelper.copyToUsbFolder(sessions, callback)
            
            if (result.isSuccess) {
                // Delete all sessions after successful copy
                var deleted = 0
                sessions.forEach { session ->
                    if (transferHelper.deleteSession(session)) {
                        deleted++
                    }
                }
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    Toast.makeText(this@FileTransferActivity, 
                        "Files copied! ($deleted sessions deleted)\nConnect USB to access", 
                        Toast.LENGTH_LONG).show()
                    loadRecordedFiles()  // Reload to update UI
                }
            }
        }
    }
    
    private fun showWiFiDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Server IP:Port"
        input.setText("10.99.0.200:8888")  // Default IP dan port
        input.selectAll()  // Select all untuk mudah diganti kalau perlu
        
        AlertDialog.Builder(this)
            .setTitle("WiFi Transfer")
            .setMessage("PC Server: 10.99.0.200:8888\n(Tap OK to use default)")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val ipPort = input.text.toString().trim()
                if (ipPort.isNotEmpty()) {
                    // Parse IP and port
                    val parts = ipPort.split(":")
                    val ip = parts[0]
                    val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 8888 else 8888
                    transferViaWiFi(ip, port)
                } else {
                    Toast.makeText(this, "Please enter IP address", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun transferViaWiFi(serverIp: String, port: Int = 8888) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgress.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val callback = object : FileTransferHelper.TransferCallback {
                override fun onTransferStarted(fileName: String) {
                    runOnUiThread {
                        binding.tvProgress.text = "Transferring: $fileName"
                    }
                }
                
                override fun onTransferProgress(fileName: String, progress: Int) {
                    runOnUiThread {
                        binding.tvProgress.text = "Transferring: $fileName ($progress%)"
                    }
                }
                
                override fun onTransferComplete(fileName: String) {
                    runOnUiThread {
                        binding.tvProgress.text = "Completed: $fileName"
                    }
                }
                
                override fun onTransferError(fileName: String, error: String) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        binding.tvProgress.visibility = View.GONE
                        Toast.makeText(this@FileTransferActivity, "Transfer failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
            
            // Transfer all files sequentially
            var successfulTransfers = 0
            for (session in sessions) {
                // Transfer video
                val videoResult = transferHelper.transferViaWiFi(session.videoFile, serverIp, port, callback).getOrNull()
                if (videoResult == null) break
                
                // Transfer NMEA if exists
                var nmeaTransferred = true
                session.nmeaFile?.let { nmeaFile ->
                    val nmeaResult = transferHelper.transferViaWiFi(nmeaFile, serverIp, port, callback).getOrNull()
                    if (nmeaResult == null) {
                        nmeaTransferred = false
                        return@launch
                    }
                }
                
                // Delete files after successful transfer
                if (nmeaTransferred) {
                    transferHelper.deleteSession(session)
                    successfulTransfers++
                }
            }
            
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                binding.tvProgress.visibility = View.GONE
                Toast.makeText(this@FileTransferActivity, 
                    "All files transferred! ($successfulTransfers sessions deleted)", 
                    Toast.LENGTH_LONG).show()
                loadRecordedFiles()  // Reload to update UI
            }
        }
    }
    
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Files")
            .setMessage("Delete all ${sessions.size} recorded sessions? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAllFiles()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteAllFiles() {
        lifecycleScope.launch {
            var deleted = 0
            sessions.forEach { session ->
                if (transferHelper.deleteSession(session)) {
                    deleted++
                }
            }
            
            runOnUiThread {
                Toast.makeText(this@FileTransferActivity, "Deleted $deleted sessions", Toast.LENGTH_SHORT).show()
                loadRecordedFiles()
            }
        }
    }
}
