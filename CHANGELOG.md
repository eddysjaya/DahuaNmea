# Changelog

All notable changes to Dahua NMEA Recorder will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-01-14

### Added
- Initial release
- Video recording using Camera2 API
- Real-time GPS tracking with FusedLocationProviderClient
- NMEA format export (GPRMC and GPGGA sentences)
- Simple UI optimized for 2.4 inch screen (240x320)
- START/STOP recording controls
- Recording status indicators
- GPS status indicators
- Real-time timer display
- GPS point counter
- Foreground service for stable recording
- Notification during recording
- Auto-generated filenames with timestamps
- Organized file storage structure
- USB file transfer support via FileProvider
- Camera preview
- Synchronized video and GPS timestamps
- Permission handling
- Error handling and user feedback
- App-specific storage (Android 11+)
- H.264 video encoding at 30fps
- 640x480 video resolution
- 1Hz GPS update rate
- NMEA checksum calculation
- Latitude/longitude formatting
- Complete documentation (README, User Guide, Installation Guide)

### Security
- Runtime permission requests for Camera, Audio, Location
- Foreground service with proper permissions
- FileProvider for secure file sharing
- App-specific storage without requiring WRITE_EXTERNAL_STORAGE

### Performance
- Efficient GPS tracking with optimized update intervals
- Background service prevents system kill
- Minimal UI for low-resource device
- Optimized for 2.4 inch display

### Compatibility
- Target: Android 11 (API 30)
- Device: Dahua MPT230 Body Worn Camera
- Screen: 240x320 (2.4 inch)

---

## [Unreleased]

### Planned Features
- File browser within app
- Share functionality
- Recording settings (quality, resolution)
- GPS accuracy filter
- Pause/resume recording
- Video thumbnail preview
- Storage usage indicator
- Auto-cleanup old files
- Export to GPX format
- Map view for GPS track
- Multiple language support
- Dark/Light theme toggle
- Recording statistics
- Battery optimization
- Cloud sync capability

### Known Issues
- UI locked to portrait mode only
- No built-in file viewer
- No recording pause functionality
- Fixed video quality settings
- No GPS accuracy threshold

---

## Version History

### Version 1.0.0 (2026-01-14)
- Initial public release for Dahua MPT230
- Core recording and GPS tracking features
- NMEA export functionality
- Basic UI and file management
