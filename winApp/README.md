# Dahua NMEA Viewer - Windows Desktop Application

A Windows desktop application to download, view, and manage video recordings with GPS tracking data from Dahua body cameras.

## Features

- **📥 Download from Device**: Automatically pull video and NMEA files from Android device via ADB
- **🗺️ GPS Track Visualization**: Display GPS tracking route on OpenStreetMap
- **📹 Video Playback**: Synchronized video player with GPS position marker
- **🗑️ Auto Cleanup**: Automatically delete files from device after successful download
- **💾 Session Management**: Browse and replay downloaded sessions

## Requirements

- Windows 10 or later
- .NET 7.0 Runtime
- ADB (Android Debug Bridge)
- USB cable for device connection

## Installation

### 1. Install .NET 7.0 Runtime
Download from: https://dotnet.microsoft.com/download/dotnet/7.0

### 2. Build the Application
```powershell
cd d:\development\DahuaNmea\winApp
dotnet restore
dotnet build
dotnet publish -c Release -r win-x64 --self-contained false
```

### 3. Run the Application
```powershell
cd bin\Release\net7.0-windows\win-x64\publish
.\DahuaNmeaViewer.exe
```

## Configuration

1. Click **⚙ Settings** button
2. Set ADB path (default: `C:\Program Files (x86)\MPTManager\MPT\adb.exe`)
3. Click **Test Connection** to verify device connectivity
4. Click **Save**

## Usage

### Download Files from Device

1. Connect Android device via USB
2. Enable **USB Debugging** on device
3. Click **📥 Download from Device**
4. Wait for download to complete
5. Files will be saved to: `Documents\DahuaNmeaViewer\Downloads`

### View Recordings

1. Select a session from the list on the left
2. Video will load in the left panel
3. GPS track will display on the map (right panel)
4. Use playback controls:
   - **▶** Play
   - **⏸** Pause
   - **⏹** Stop
5. GPS marker syncs with video playback position

## File Structure

```
Downloads/
├── video1.mp4
├── video1.txt (NMEA)
├── video2.mp4
└── video2.txt (NMEA)
```

## Device File Paths

- **Videos**: `/storage/emulated/0/mpt/`
- **NMEA**: `/storage/emulated/0/Android/data/com.dahua.nmea/files/NMEA/`

## Troubleshooting

### "No device connected"
- Check USB cable connection
- Enable USB Debugging in Developer Options
- Run `adb devices` in command prompt to verify

### "ADB command failed"
- Verify ADB path in Settings
- Install Android SDK Platform Tools
- Check device drivers

### "No GPS data"
- Ensure GPS was enabled during recording
- Check if NMEA file contains GPS points
- Verify file is not empty or corrupted

## Development

### Project Structure
```
winApp/
├── DahuaNmeaViewer.csproj
├── App.xaml / App.xaml.cs
├── MainWindow.xaml / MainWindow.xaml.cs
├── SettingsWindow.xaml / SettingsWindow.xaml.cs
├── AdbManager.cs
├── NmeaParser.cs
└── MapHtmlGenerator.cs
```

### Dependencies
- Microsoft.Web.WebView2 (for map rendering)
- Newtonsoft.Json (for JSON handling)

## License

MIT License - See mobile app for details
