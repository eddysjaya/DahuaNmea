# Gradle Build Instruction

## Build Commands

### Debug Build
```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

### Release Build
```bash
# Windows
gradlew.bat assembleRelease

# Linux/Mac
./gradlew assembleRelease
```

### Install to Device
```bash
# Windows
gradlew.bat installDebug

# Linux/Mac
./gradlew installDebug
```

### Clean Build
```bash
# Windows
gradlew.bat clean build

# Linux/Mac
./gradlew clean build
```

## Build Requirements

- JDK 8 or higher
- Android SDK (API 30+)
- Gradle 8.0+

## Output Location

APK files will be generated in:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`
