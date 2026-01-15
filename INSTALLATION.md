# Panduan Instalasi Dahua NMEA Recorder

## Persiapan

### Perangkat yang Dibutuhkan
- Dahua MPT230 Body Worn Camera (atau perangkat Android 11 dengan layar 2.4")
- PC dengan Android Studio (untuk development)
- Kabel USB untuk transfer data
- Koneksi internet untuk download dependencies

### Software Requirements
- Android Studio Arctic Fox (2020.3.1) atau lebih baru
- JDK 8 atau lebih tinggi
- Android SDK API 30 (Android 11)
- Gradle 8.0+

## Langkah Instalasi

### 1. Setup Development Environment

#### Install Android Studio
1. Download Android Studio dari https://developer.android.com/studio
2. Install Android Studio
3. Buka Android Studio dan setup SDK:
   - Tools → SDK Manager
   - Install Android 11.0 (R) / API Level 30
   - Install Android SDK Build-Tools 33.0.0

#### Install JDK
1. Pastikan JDK 8+ sudah terinstall
2. Cek dengan command: `java -version`
3. Set JAVA_HOME environment variable

### 2. Setup Project

#### Clone/Extract Project
```bash
cd d:\development\
# Project sudah ada di DahuaNmea folder
```

#### Open di Android Studio
1. Buka Android Studio
2. File → Open
3. Pilih folder `d:\development\DahuaNmea`
4. Tunggu Gradle sync selesai (pertama kali akan download dependencies)

### 3. Build Application

#### Via Android Studio
1. Build → Make Project (Ctrl+F9)
2. Tunggu build selesai
3. Check output di "Build" tab

#### Via Command Line
```bash
cd d:\development\DahuaNmea

# Windows
gradlew.bat clean assembleDebug

# Linux/Mac
./gradlew clean assembleDebug
```

### 4. Install ke Perangkat

#### Persiapan Perangkat
1. Aktifkan Developer Options:
   - Settings → About Phone
   - Tap "Build Number" 7 kali
2. Aktifkan USB Debugging:
   - Settings → Developer Options
   - Enable "USB Debugging"
3. Hubungkan perangkat ke PC via USB
4. Izinkan USB Debugging di perangkat

#### Install APK

**Via Android Studio:**
1. Run → Select Device
2. Pilih perangkat Dahua MPT230
3. Klik Run (Shift+F10)

**Via Command Line:**
```bash
# Install debug version
gradlew.bat installDebug

# Atau manual via ADB
adb install app\build\outputs\apk\debug\app-debug.apk
```

**Via File Transfer:**
1. Copy APK dari `app\build\outputs\apk\debug\app-debug.apk`
2. Transfer ke perangkat via USB
3. Buka File Manager di perangkat
4. Tap APK file dan install

### 5. First Run Setup

1. **Launch App**
   - Tap icon "Dahua NMEA Recorder" di home screen

2. **Grant Permissions**
   Aplikasi akan meminta izin berikut, izinkan semua:
   - ✅ Camera
   - ✅ Microphone
   - ✅ Location
   - ✅ Files and Media (jika diminta)

3. **Test Recording**
   - Tap tombol "START"
   - Tunggu GPS lock (indikator GPS akan berubah hijau)
   - Biarkan recording beberapa detik
   - Tap tombol "STOP"

4. **Verify Files**
   - Buka File Manager
   - Navigate ke: Internal Storage/Android/data/com.dahua.nmea/files/DahuaNmea/
   - Check folder "Videos" (ada file .mp4)
   - Check folder "NMEA" (ada file .nmea)

## Troubleshooting Installation

### Gradle Sync Failed
```bash
# Clear Gradle cache
gradlew.bat --stop
gradlew.bat clean

# Delete .gradle folder
rmdir /s .gradle
```

### Build Failed
- Check JDK version: `java -version`
- Update Android SDK via SDK Manager
- Invalidate Caches: File → Invalidate Caches / Restart

### Device Not Detected
```bash
# Check ADB connection
adb devices

# Restart ADB
adb kill-server
adb start-server
```

### Installation Failed
- Uninstall existing app first
- Check storage space
- Enable "Install from Unknown Sources"

### Permissions Not Working
- Settings → Apps → Dahua NMEA Recorder → Permissions
- Enable all permissions manually

## Build Variants

### Debug Build (untuk testing)
```bash
gradlew.bat assembleDebug
# Output: app-debug.apk
# Signed with debug key
# Includes debugging info
```

### Release Build (untuk production)
```bash
gradlew.bat assembleRelease
# Output: app-release-unsigned.apk
# Perlu signing dengan production key
# Optimized & minified
```

## Signing Release APK

### Generate Keystore
```bash
keytool -genkey -v -keystore dahua-release.keystore -alias dahua -keyalg RSA -keysize 2048 -validity 10000
```

### Configure Signing in build.gradle
```gradle
android {
    signingConfigs {
        release {
            storeFile file("dahua-release.keystore")
            storePassword "your-password"
            keyAlias "dahua"
            keyPassword "your-password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

### Build Signed Release
```bash
gradlew.bat assembleRelease
# Output: app-release.apk (signed)
```

## Deployment

### Via USB Mass Install
1. Copy APK ke shared folder
2. Connect semua perangkat
3. Use batch install script:
```bash
@echo off
for /f "tokens=1" %%D in ('adb devices ^| findstr device$') do (
    echo Installing to %%D
    adb -s %%D install -r app-debug.apk
)
```

### Via OTA Update
1. Upload APK ke server
2. Implement OTA update mechanism
3. Push update notification

## Maintenance

### Update Dependencies
```gradle
// Update di app/build.gradle
dependencies {
    implementation 'androidx.core:core-ktx:1.10.1' // Update version
    // ...
}
```

### Update Android Version
```gradle
android {
    compileSdk 34  // Update
    targetSdk 34   // Update
}
```

## Support

Untuk bantuan lebih lanjut:
- Check README.md untuk dokumentasi lengkap
- Check BUILD.md untuk build commands
- Contact: Dahua Technology Support Team
