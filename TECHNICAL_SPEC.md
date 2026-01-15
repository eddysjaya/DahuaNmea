# Dahua NMEA Recorder - Technical Specification

## System Architecture

### Overview
Aplikasi Android berbasis Kotlin yang mengintegrasikan perekaman video dengan GPS tracking real-time, menghasilkan output video MP4 dan file NMEA untuk analisis geolocation.

## Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        MainActivity                          │
│  - UI Management                                            │
│  - Permission Handling                                      │
│  - Camera Preview Setup                                     │
│  - Service Binding                                          │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  │ Bind/Unbind
                  │
┌─────────────────▼───────────────────────────────────────────┐
│                    RecordingService                          │
│  (Foreground Service)                                       │
│                                                              │
│  ┌────────────────┐  ┌──────────────┐  ┌────────────────┐ │
│  │ Camera Manager │  │  GPS Tracker │  │ NMEA Generator │ │
│  │                │  │              │  │                │ │
│  │ - Camera2 API  │  │ - Fused Loc  │  │ - GPRMC/GPGGA  │ │
│  │ - MediaRecorder│  │ - 1Hz Update │  │ - Checksum     │ │
│  │ - Video Output │  │ - Callbacks  │  │ - File Write   │ │
│  └────────────────┘  └──────────────┘  └────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                  File Manager                          │ │
│  │  - File Creation                                       │ │
│  │  - Directory Management                                │ │
│  │  - Storage Access                                      │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

## Data Flow

### Recording Start Flow
```
User Taps START
      ↓
MainActivity.startRecording()
      ↓
Start & Bind RecordingService
      ↓
RecordingService.startRecording()
      ↓
├─→ FileManager.createVideoFile()
├─→ FileManager.createNmeaFile()
├─→ GpsTracker.startTracking()
├─→ NmeaGenerator.init()
└─→ CameraManager.startVideoRecording()
      ↓
RecordingCallback.onRecordingStarted()
      ↓
Update UI (indicator, buttons, status)
```

### Recording Loop
```
┌───────────────────────────────────────┐
│   RecordingService (Active)           │
│                                        │
│   Every 1 second:                     │
│   ┌─────────────────────────────────┐ │
│   │ GPS Update                       │ │
│   │   ↓                              │ │
│   │ Location → NmeaGenerator         │ │
│   │   ↓                              │ │
│   │ Generate GPRMC sentence          │ │
│   │   ↓                              │ │
│   │ Generate GPGGA sentence          │ │
│   │   ↓                              │ │
│   │ Write to NMEA file               │ │
│   │   ↓                              │ │
│   │ Callback → Update UI             │ │
│   └─────────────────────────────────┘ │
│                                        │
│   Continuously:                        │
│   ┌─────────────────────────────────┐ │
│   │ Video Recording                  │ │
│   │   ↓                              │ │
│   │ Camera frames → MediaRecorder    │ │
│   │   ↓                              │ │
│   │ Encode H.264                     │ │
│   │   ↓                              │ │
│   │ Write to MP4 file                │ │
│   └─────────────────────────────────┘ │
│                                        │
│   Every 1 second:                     │
│   ┌─────────────────────────────────┐ │
│   │ Timer Update                     │ │
│   │   ↓                              │ │
│   │ Calculate elapsed time           │ │
│   │   ↓                              │ │
│   │ Callback → Update UI timer       │ │
│   └─────────────────────────────────┘ │
└───────────────────────────────────────┘
```

### Recording Stop Flow
```
User Taps STOP
      ↓
MainActivity.stopRecording()
      ↓
RecordingService.stopRecording()
      ↓
├─→ MediaRecorder.stop()
├─→ MediaRecorder.release()
├─→ GpsTracker.stopTracking()
├─→ NmeaGenerator.close()
└─→ CameraDevice.close()
      ↓
RecordingCallback.onRecordingStopped()
      ↓
Update UI (reset status)
      ↓
Unbind Service
      ↓
Service.stopSelf()
```

## Technical Specifications

### Video Recording

**Camera Configuration**:
```kotlin
MediaRecorder.apply {
    setAudioSource(AudioSource.MIC)
    setVideoSource(VideoSource.SURFACE)
    setOutputFormat(OutputFormat.MPEG_4)
    setVideoEncoder(VideoEncoder.H264)
    setAudioEncoder(AudioEncoder.AAC)
    setVideoEncodingBitRate(2000000)  // 2 Mbps
    setVideoFrameRate(30)              // 30 fps
    setVideoSize(640, 480)             // VGA resolution
}
```

**Performance**:
- Bitrate: 2 Mbps (adjustable)
- Frame Rate: 30 fps
- Resolution: 640x480 (VGA)
- Codec: H.264 (hardware accelerated)
- Audio: AAC, 44.1kHz
- File Size: ~10-15 MB/minute

### GPS Tracking

**Location Configuration**:
```kotlin
LocationRequest.Builder(
    Priority.PRIORITY_HIGH_ACCURACY,
    1000L  // 1 second interval
).apply {
    setMinUpdateIntervalMillis(500L)     // Min 0.5 second
    setMaxUpdateDelayMillis(2000L)       // Max 2 second
}.build()
```

**Accuracy**:
- Mode: High Accuracy (GPS + Network)
- Update Rate: 1 Hz (every 1 second)
- Min Interval: 0.5 seconds
- Max Delay: 2 seconds
- Typical Accuracy: 5-10 meters (outdoor)

### NMEA Generation

**Supported Sentences**:

1. **GPRMC** (Recommended Minimum Specific GPS/Transit Data)
   ```
   $GPRMC,hhmmss.sss,A,ddmm.mmmm,N/S,dddmm.mmmm,E/W,speed,track,ddmmyy,,,*hh
   ```
   Fields:
   - Time (UTC)
   - Status (A=Active, V=Void)
   - Latitude (degrees + minutes)
   - North/South indicator
   - Longitude (degrees + minutes)
   - East/West indicator
   - Speed over ground (knots)
   - Track angle (degrees)
   - Date (DDMMYY)
   - Checksum

2. **GPGGA** (Global Positioning System Fix Data)
   ```
   $GPGGA,hhmmss.sss,ddmm.mmmm,N/S,dddmm.mmmm,E/W,q,ss,h.h,alt,M,geo,M,,*hh
   ```
   Fields:
   - Time (UTC)
   - Latitude
   - North/South
   - Longitude
   - East/West
   - Fix quality (0-8)
   - Number of satellites
   - HDOP
   - Altitude (meters)
   - Geoidal separation
   - Checksum

**Checksum Calculation**:
```kotlin
fun calculateChecksum(sentence: String): String {
    var checksum = 0
    for (char in sentence) {
        checksum = checksum xor char.code
    }
    return "%02X".format(checksum)
}
```

## Threading Model

### Main Thread
- UI updates
- User interactions
- Camera preview

### Background Thread (Service)
- Video recording
- GPS tracking
- File I/O
- NMEA generation

### Location Callback Thread
- GPS location updates
- NMEA writing

## Memory Management

### Memory Usage Estimate
- App Base: ~50-80 MB
- Camera Preview: ~10-20 MB
- Video Recording: ~5-10 MB buffer
- GPS Service: ~2-5 MB
- Total: ~70-115 MB

### Memory Optimization
- Reuse objects where possible
- Release camera resources promptly
- Close file streams properly
- Unbind services when not needed
- Clear callbacks on destroy

## Storage Management

### File Structure
```
Internal Storage/
└── Android/
    └── data/
        └── com.dahua.nmea/
            └── files/
                └── DahuaNmea/
                    ├── Videos/
                    │   ├── VID_20260114_120000.mp4
                    │   ├── VID_20260114_123000.mp4
                    │   └── ...
                    └── NMEA/
                        ├── GPS_20260114_120000.nmea
                        ├── GPS_20260114_123000.nmea
                        └── ...
```

### Storage Requirements
- Video: ~10-15 MB/minute
- NMEA: ~5 KB/minute
- 1 Hour Recording: ~600-900 MB video + ~300 KB NMEA
- Recommended Free Space: 1 GB minimum

## Power Management

### Battery Optimization
- Use Foreground Service (prevents system kill)
- Wake Lock during recording only
- Efficient GPS update intervals
- Hardware-accelerated video encoding

### Power Consumption Estimate
- Video Recording: ~500-800 mAh/hour
- GPS Active: ~50-100 mAh/hour
- Total: ~550-900 mAh/hour
- On 3000mAh battery: ~3-5 hours continuous recording

## Network & Connectivity

### No Network Required
- App works completely offline
- No internet connection needed
- No cloud sync (local storage only)
- USB transfer for data export

### Future: Optional Network Features
- Cloud backup
- Remote monitoring
- OTA updates
- Data sync

## Security & Privacy

### Data Protection
- App-specific storage (no public access)
- FileProvider for secure sharing
- No data transmission
- Local storage only

### Permissions
- Runtime permissions required
- Explicit user consent
- Minimal permission scope
- Revocable at any time

## Performance Metrics

### Target Performance
- App Launch: < 2 seconds
- Camera Ready: < 1 second
- GPS First Fix (cold): < 60 seconds
- GPS First Fix (warm): < 15 seconds
- Recording Start Latency: < 1 second
- Recording Stop Latency: < 2 seconds
- UI Response Time: < 100ms

### Benchmarks
| Operation | Target | Typical |
|-----------|--------|---------|
| App Start | < 2s | 1-1.5s |
| GPS Lock | < 60s | 10-30s |
| Record Start | < 1s | 0.5s |
| Record Stop | < 2s | 1s |
| File Save | < 3s | 1-2s |

## Error Handling

### Common Errors

1. **Camera Error**
   - Cause: Camera in use, no permission
   - Handling: Release camera, show error, request permission

2. **GPS Error**
   - Cause: No signal, permission denied
   - Handling: Show searching status, request permission

3. **Storage Error**
   - Cause: No space, permission denied
   - Handling: Check space, show error, request permission

4. **Service Error**
   - Cause: System kill, low memory
   - Handling: Restart service, save partial data

### Error Recovery
- Graceful degradation
- User notification
- Automatic retry (where appropriate)
- Save partial data when possible

## Testing Strategy

### Unit Tests
- NMEA generation algorithms
- File naming logic
- Checksum calculation
- Coordinate conversion

### Integration Tests
- Camera + GPS integration
- Service lifecycle
- File I/O operations
- Permission handling

### Manual Tests
- Full recording workflow
- Various GPS conditions
- Battery scenarios
- Storage scenarios
- Permission scenarios

## Future Enhancements

### Planned Features
1. **Video Quality Settings**
   - Resolution: 480p, 720p, 1080p
   - Bitrate: Low, Medium, High
   - Frame Rate: 24, 30, 60 fps

2. **GPS Features**
   - Accuracy filter
   - Custom update intervals
   - GPX export
   - Map overlay

3. **UI Improvements**
   - File browser
   - Video preview
   - Statistics dashboard
   - Settings screen

4. **File Management**
   - Auto cleanup old files
   - Cloud backup
   - Batch operations
   - Search/filter

5. **Advanced Features**
   - Motion detection
   - Audio analysis
   - Live streaming
   - Remote control

## Appendix

### Dependencies Version Matrix
| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | 1.9.0 | Language |
| AndroidX Core | 1.10.1 | Core Android |
| CameraX | 1.2.3 | Camera API |
| Play Services | 21.0.1 | Location |
| Coroutines | 1.7.1 | Async |

### Build Variants
- **Debug**: Development build with logging
- **Release**: Production build, optimized

### Gradle Tasks
```bash
./gradlew tasks --all  # List all tasks
./gradlew dependencies # Show dependencies
./gradlew lint        # Run lint checks
```

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-14  
**Author**: Dahua Technology Development Team
