# Gradle Setup & Fix Guide

## Error: Gradle Wrapper Not Found

### Solusi 1: Generate Gradle Wrapper (Recommended)

Jika Anda memiliki Gradle installed di sistem:

```bash
# Pastikan di folder project
cd d:\development\DahuaNmea

# Generate gradle wrapper
gradle wrapper --gradle-version 8.9
```

### Solusi 2: Download Manual

1. Download gradle-wrapper.jar dari:
   https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.9/gradle-wrapper-8.9.jar

2. Rename menjadi `gradle-wrapper.jar`

3. Copy ke folder:
   ```
   d:\development\DahuaNmea\gradle\wrapper\gradle-wrapper.jar
   ```

### Solusi 3: Menggunakan Android Studio

1. Buka project di Android Studio
2. Android Studio akan otomatis download Gradle wrapper
3. Tunggu sync selesai

## Error: Gradle Version Incompatibility

### Sudah Diperbaiki ✅

Versi telah diupdate ke:
- **Gradle**: 8.9
- **Android Gradle Plugin**: 8.2.2
- **Kotlin**: 1.9.22

### Versi Kompatibilitas

| Gradle | Android Gradle Plugin | Kotlin |
|--------|----------------------|--------|
| 8.9    | 8.2.2               | 1.9.22 |
| 8.2    | 8.1.0               | 1.9.0  |
| 8.0    | 8.0.0               | 1.8.20 |

## Error: Build Failed

### Check JDK Version
```bash
java -version
```

Pastikan JDK 8 atau lebih tinggi terinstall.

### Clear Cache & Rebuild
```bash
# Stop Gradle daemon
.\gradlew.bat --stop

# Clean project
.\gradlew.bat clean

# Build project
.\gradlew.bat build
```

### Delete .gradle folder
```bash
# Delete cache
Remove-Item -Path ".gradle" -Recurse -Force

# Rebuild
.\gradlew.bat build
```

## Error: Dependencies Download Failed

### Check Internet Connection
Pastikan koneksi internet aktif untuk download dependencies.

### Use Gradle Offline Mode (jika sudah pernah build)
```bash
.\gradlew.bat build --offline
```

### Clear Dependency Cache
```bash
# Delete dependency cache
Remove-Item -Path "$env:USERPROFILE\.gradle\caches" -Recurse -Force

# Rebuild
.\gradlew.bat build
```

## Quick Fix Commands

### Build Debug APK
```bash
cd d:\development\DahuaNmea
.\gradlew.bat assembleDebug
```

### Install to Device
```bash
.\gradlew.bat installDebug
```

### Check Gradle Tasks
```bash
.\gradlew.bat tasks
```

### Check Dependencies
```bash
.\gradlew.bat dependencies
```

## Common Errors & Solutions

### 1. "gradlew.bat is not recognized"
**Cause**: File tidak executable atau tidak ada
**Fix**: Pastikan file `gradlew.bat` ada di root project

### 2. "Could not find method implementation()"
**Cause**: Gradle version terlalu lama
**Fix**: Update Gradle wrapper ke versi terbaru (8.9)

### 3. "Failed to apply plugin 'com.android.application'"
**Cause**: AGP version tidak kompatibel
**Fix**: Sudah diperbaiki - AGP 8.2.2 kompatibel dengan Gradle 8.9

### 4. "Unsupported class file major version"
**Cause**: JDK version tidak cocok
**Fix**: Install JDK 17 atau gunakan JDK 11

### 5. "Could not resolve all dependencies"
**Cause**: Network issue atau repository tidak tersedia
**Fix**: 
```bash
# Check repositories di build.gradle
# Pastikan google() dan mavenCentral() ada
```

## Build dari Android Studio

### Method 1: Sync & Build
1. Open project di Android Studio
2. File → Sync Project with Gradle Files
3. Build → Make Project (Ctrl+F9)

### Method 2: Clean & Rebuild
1. Build → Clean Project
2. Build → Rebuild Project

### Method 3: Invalidate Caches
1. File → Invalidate Caches / Restart
2. Select "Invalidate and Restart"
3. Wait for reindexing
4. Build project

## Verify Setup

### Check Build Files
```bash
# Should exist:
.\gradlew.bat
.\gradlew
.\gradle\wrapper\gradle-wrapper.properties
.\gradle\wrapper\gradle-wrapper.jar (after first build)
.\build.gradle
.\settings.gradle
.\app\build.gradle
```

### Run Tests
```bash
.\gradlew.bat test
```

### List Build Variants
```bash
.\gradlew.bat tasks --group="build"
```

## Production Build

### Create Release APK
```bash
# Build release
.\gradlew.bat assembleRelease

# Output:
# app\build\outputs\apk\release\app-release-unsigned.apk
```

### Sign APK (if needed)
```bash
# Generate keystore (first time only)
keytool -genkey -v -keystore release.keystore -alias dahua -keyalg RSA -keysize 2048 -validity 10000

# Build signed release
.\gradlew.bat assembleRelease
```

## Need Help?

### Check Build Output
```bash
.\gradlew.bat build --stacktrace
.\gradlew.bat build --info
.\gradlew.bat build --debug
```

### Check Gradle Version
```bash
.\gradlew.bat --version
```

### Check Java Version
```bash
java -version
javac -version
```

## Current Project Configuration ✅

```groovy
// Root build.gradle
plugins {
    id 'com.android.application' version '8.2.2'
    id 'org.jetbrains.kotlin.android' version '1.9.22'
}

// gradle-wrapper.properties
distributionUrl=gradle-8.9-bin.zip

// app/build.gradle
android {
    compileSdk 33
    minSdk 30
    targetSdk 33
}
```

**Status**: ✅ Konfigurasi sudah diperbaiki dan kompatibel!

## Next Steps

1. Generate Gradle Wrapper (pilih salah satu method di atas)
2. Sync project di Android Studio ATAU run `.\gradlew.bat build`
3. Build APK: `.\gradlew.bat assembleDebug`
4. Install: `.\gradlew.bat installDebug`

---

**Last Updated**: 2026-01-14  
**Gradle Version**: 8.9  
**AGP Version**: 8.2.2  
**Kotlin Version**: 1.9.22
