package com.dahua.nmea

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dahua.nmea.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 101
        private const val REQUEST_CODE_STORAGE = 102
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        if (!checkPermissions()) {
            requestPermissions()
        }
        
        // Request MANAGE_EXTERNAL_STORAGE for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestStoragePermission()
            }
        }
        
        setupUI()
    }
    
    private fun requestStoragePermission() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Required")
            .setMessage("This app needs all files access permission to delete DSJ Camera videos.\n\nPlease enable 'Allow access to manage all files' in the next screen.")
            .setPositiveButton("Grant") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivityForResult(intent, REQUEST_CODE_STORAGE)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivityForResult(intent, REQUEST_CODE_STORAGE)
                    }
                }
            }
            .setNegativeButton("Skip", null)
            .show()
    }
    
    private fun setupUI() {
        // Launch DSJ Camera button
        binding.btnLaunchCamera.setOnClickListener {
            launchDSJCamera()
        }
        
        // Transfer button
        binding.btnTransfer.setOnClickListener {
            val intent = Intent(this, FileTransferActivity::class.java)
            startActivity(intent)
        }
        
        // DSJ Monitor toggle
        binding.switchDsjMonitor.isChecked = isDsjMonitorServiceRunning()
        binding.switchDsjMonitor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isGpsEnabled()) {
                    showGpsDisabledDialog()
                    binding.switchDsjMonitor.isChecked = false
                    return@setOnCheckedChangeListener
                }
                startDsjMonitor()
            } else {
                stopDsjMonitor()
            }
        }
    }
    
    private fun launchDSJCamera() {
        try {
            val intent = Intent().apply {
                setClassName("com.dsj.app", "com.dsj.app.Camera")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open DSJ Camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startDsjMonitor() {
        if (!checkPermissions()) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            binding.switchDsjMonitor.isChecked = false
            return
        }
        
        try {
            val intent = Intent(this, DsjCameraMonitorService::class.java)
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "DSJ Auto Track started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.switchDsjMonitor.isChecked = false
        }
    }
    
    private fun stopDsjMonitor() {
        try {
            val intent = Intent(this, DsjCameraMonitorService::class.java)
            stopService(intent)
            Toast.makeText(this, "DSJ Auto Track stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    private fun isDsjMonitorServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (DsjCameraMonitorService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
    
    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }
    
    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    
    private fun showGpsDisabledDialog() {
        AlertDialog.Builder(this)
            .setTitle("GPS Disabled")
            .setMessage("GPS is required for tracking. Please enable GPS/Location Services.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!checkPermissions()) {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Storage permission denied - delete may not work for DSJ videos", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
