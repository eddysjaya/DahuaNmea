# Dahua NMEA Recorder - Project Summary

## 📱 Informasi Aplikasi

**Nama Aplikasi**: Dahua NMEA Recorder  
**Package Name**: com.dahua.nmea  
**Version**: 1.0  
**Target Device**: Dahua MPT230 Body Worn Camera  
**Android Version**: Android 11 (API 30)  
**Screen Size**: 2.4 inch (240x320)  

## 🎯 Fitur Utama

✅ **Video Recording**
- HD video recording menggunakan Camera2 API
- Format: MP4 (H.264 codec)
- Resolusi: 640x480
- Frame rate: 30fps
- Audio recording included

✅ **GPS Tracking**
- Real-time GPS tracking menggunakan FusedLocationProviderClient
- Update interval: 1 second
- High accuracy mode
- Format output: NMEA 0183

✅ **NMEA Export**
- GPRMC sentence (Recommended Minimum)
- GPGGA sentence (Fix Data)
- Timestamps synchronized dengan video
- Text file format (.nmea)

✅ **Simple UI**
- Optimized untuk layar kecil 2.4"
- Camera preview
- START/STOP buttons
- Status indicators (recording, GPS, timer)
- Real-time updates

✅ **File Management**
- Auto-generate filenames dengan timestamp
- Organized folder structure
- USB file transfer support
- App-specific storage (Android 11+)

✅ **Background Service**
- Foreground service untuk recording stability
- Notification saat recording
- Prevents system kill
- Multi-threading untuk performa

## 📁 Struktur Proyek

```
DahuaNmea/
├── .github/
│   └── instructions/
│       └── codacy.instructions.md
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/dahua/nmea/
│   │       │   ├── MainActivity.kt                    # Main activity
│   │       │   ├── service/
│   │       │   │   └── RecordingService.kt            # Foreground service
│   │       │   └── utils/
│   │       │       ├── FileManager.kt                 # File operations
│   │       │       ├── GpsTracker.kt                  # GPS tracking
│   │       │       ├── NmeaGenerator.kt               # NMEA conversion
│   │       │       └── UsbTransferHelper.kt           # USB transfer
│   │       ├── res/
│   │       │   ├── drawable/
│   │       │   │   ├── circle_gray.xml                # Status indicator
│   │       │   │   ├── circle_red.xml                 # Recording indicator
│   │       │   │   ├── ic_gps.xml                     # GPS icon
│   │       │   │   └── ic_launcher_foreground.xml     # Launcher icon
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml              # Main UI layout
│   │       │   ├── mipmap-anydpi-v26/
│   │       │   │   ├── ic_launcher.xml
│   │       │   │   └── ic_launcher_round.xml
│   │       │   ├── values/
│   │       │   │   ├── colors.xml
│   │       │   │   ├── ic_launcher_background.xml
│   │       │   │   ├── strings.xml
│   │       │   │   └── themes.xml
│   │       │   └── xml/
│   │       │       └── file_paths.xml                 # FileProvider config
│   │       └── AndroidManifest.xml                    # App manifest
│   ├── build.gradle                                   # App build config
│   └── proguard-rules.pro                             # ProGuard rules
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties                  # Gradle wrapper
├── build.gradle                                       # Project build config
├── gradle.properties                                  # Gradle properties
├── settings.gradle                                    # Project settings
├── .gitignore                                         # Git ignore
├── README.md                                          # Main documentation
├── BUILD.md                                           # Build instructions
├── INSTALLATION.md                                    # Installation guide
├── USER_GUIDE.md                                      # User manual
└── PROJECT_SUMMARY.md                                 # This file
```

## 🔧 Teknologi yang Digunakan

### Android Framework
- **Language**: Kotlin
- **Min SDK**: 30 (Android 11)
- **Target SDK**: 33
- **Compile SDK**: 33

### Libraries & Dependencies
- **AndroidX Core KTX** 1.10.1
- **AndroidX AppCompat** 1.6.1
- **Material Components** 1.9.0
- **ConstraintLayout** 2.1.4
- **CameraX Core** 1.2.3
- **CameraX Camera2** 1.2.3
- **CameraX Lifecycle** 1.2.3
- **CameraX Video** 1.2.3
- **CameraX View** 1.2.3
- **Play Services Location** 21.0.1
- **Kotlin Coroutines Android** 1.7.1
- **Kotlin Coroutines Play Services** 1.7.1

### Build Tools
- **Gradle**: 8.0
- **Android Gradle Plugin**: 8.1.0
- **Kotlin Plugin**: 1.9.0

## 📋 Permissions

### Required Permissions
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.USB_PERMISSION" />
```

## 🗂️ Output Files

### Video Files
- **Location**: `/Android/data/com.dahua.nmea/files/DahuaNmea/Videos/`
- **Format**: `VID_YYYYMMDD_HHMMSS.mp4`
- **Codec**: H.264
- **Resolution**: 640x480
- **FPS**: 30
- **Size**: ~10-15 MB per minute

### NMEA Files
- **Location**: `/Android/data/com.dahua.nmea/files/DahuaNmea/NMEA/`
- **Format**: `GPS_YYYYMMDD_HHMMSS.nmea`
- **Standard**: NMEA 0183
- **Sentences**: GPRMC, GPGGA
- **Update Rate**: 1 Hz
- **Size**: ~5 KB per minute

## 🚀 Cara Build & Deploy

### Quick Start
```bash
# Clone/Open project
cd d:\development\DahuaNmea

# Build debug APK
gradlew.bat assembleDebug

# Install ke device
gradlew.bat installDebug

# Output APK
app\build\outputs\apk\debug\app-debug.apk
```

### Build Commands
```bash
# Clean build
gradlew.bat clean

# Build debug
gradlew.bat assembleDebug

# Build release
gradlew.bat assembleRelease

# Install debug
gradlew.bat installDebug

# Run tests
gradlew.bat test

# Lint check
gradlew.bat lint
```

## 📚 Dokumentasi

| File | Deskripsi |
|------|-----------|
| **README.md** | Dokumentasi utama, overview aplikasi |
| **INSTALLATION.md** | Panduan instalasi lengkap |
| **USER_GUIDE.md** | Panduan penggunaan untuk end-user |
| **BUILD.md** | Build commands reference |
| **PROJECT_SUMMARY.md** | Summary proyek (file ini) |

## 🎨 UI Components

### MainActivity
- **PreviewView**: Camera preview
- **Recording Indicator**: Bulat (gray/red)
- **Status Text**: "Ready" / "Recording..." / "Stopped"
- **Timer Display**: HH:MM:SS format
- **GPS Status**: Icon + text status
- **GPS Counter**: Jumlah titik GPS
- **START Button**: Hijau, mulai recording
- **STOP Button**: Merah, stop recording

### RecordingService
- **Foreground Notification**: Menampilkan status recording
- **Camera Management**: Open/close camera device
- **MediaRecorder**: Video recording
- **GPS Tracking**: Background location updates
- **NMEA Generation**: Real-time NMEA writing
- **Callbacks**: Update UI dengan data terbaru

## 🔄 Application Flow

```
[App Start]
    ↓
[Request Permissions] → [Denied] → [Exit]
    ↓ [Granted]
[Setup Camera Preview]
    ↓
[Wait for User Action]
    ↓
[User Taps START]
    ↓
[Start RecordingService]
    ↓
[Initialize Components]
├── Setup MediaRecorder
├── Start GPS Tracking
├── Create Output Files
└── Start NMEA Generation
    ↓
[Recording Active]
├── Video Recording
├── GPS Tracking (1 Hz)
├── NMEA Writing
├── UI Updates
└── Timer Running
    ↓
[User Taps STOP]
    ↓
[Stop All Components]
├── Stop MediaRecorder
├── Stop GPS Tracking
├── Close NMEA File
└── Save Files
    ↓
[Show Success Message]
    ↓
[Ready for Next Recording]
```

## 🧪 Testing Checklist

### Pre-recording Tests
- [ ] App launches successfully
- [ ] Permissions granted
- [ ] Camera preview visible
- [ ] GPS status shows "Searching..."
- [ ] UI buttons responsive

### Recording Tests
- [ ] START button memulai recording
- [ ] Recording indicator berubah merah
- [ ] Timer mulai berjalan
- [ ] GPS tracking aktif
- [ ] GPS counter bertambah
- [ ] STOP button berfungsi
- [ ] Files tersimpan dengan benar

### File Tests
- [ ] Video file exists (.mp4)
- [ ] Video playable
- [ ] NMEA file exists (.nmea)
- [ ] NMEA format valid
- [ ] Timestamps synchronized
- [ ] Files dapat di-copy ke PC

### Edge Cases
- [ ] Recording dengan GPS tidak lock
- [ ] Recording dengan low battery
- [ ] Recording dengan low storage
- [ ] Multiple recording sessions
- [ ] App minimize/maximize saat recording
- [ ] Device rotation
- [ ] Incoming call saat recording

## 🐛 Known Issues & Limitations

### Current Limitations
1. **UI tidak support landscape mode** (locked portrait)
2. **Tidak ada built-in file viewer** (perlu external app)
3. **Tidak ada recording pause/resume** (only start/stop)
4. **Tidak ada video quality settings** (fixed 640x480)
5. **Tidak ada GPS accuracy threshold** (record semua points)

### Future Improvements
- [ ] Add file browser dalam app
- [ ] Add share functionality
- [ ] Add recording settings (quality, resolution)
- [ ] Add GPS accuracy filter
- [ ] Add pause/resume recording
- [ ] Add video thumbnail preview
- [ ] Add storage usage indicator
- [ ] Add auto-cleanup old files
- [ ] Add export to GPX format
- [ ] Add map view for GPS track

## 📞 Support & Contact

**Developer**: Dahua Technology  
**Email**: support@dahuatech.com  
**Website**: www.dahuatech.com  

**Project Location**: `d:\development\DahuaNmea`  
**Last Updated**: January 14, 2026  
**Version**: 1.0  

## 📄 License

Copyright © 2026 Dahua Technology. All rights reserved.

---

**Note**: Aplikasi ini didesain khusus untuk Dahua MPT230 Body Worn Camera dengan layar 2.4 inch (240x320). Untuk device lain, mungkin perlu adjustment pada UI layout.
