# Test ADB Download
$adb = "C:\Program Files (x86)\MPTManager\MPT\adb.exe"
$targetDir = "d:\development\DahuaNmea\winApp\Data"

# Create target directory
if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir | Out-Null
}

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Testing ADB Download" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Check device
Write-Host "Checking device..." -ForegroundColor Yellow
& $adb devices
Write-Host ""

# Find video files
Write-Host "Finding video files..." -ForegroundColor Yellow
$files = & $adb shell "find /storage/emulated/0/mpt/ -name '*.mp4' -type f 2>/dev/null"
$videoFiles = $files -split "`n" | Where-Object { $_ -match '\.mp4$' }

Write-Host "Found $($videoFiles.Count) video(s)" -ForegroundColor Green
Write-Host ""

# Download first video
if ($videoFiles.Count -gt 0) {
    $videoPath = $videoFiles[0].Trim()
    $fileName = Split-Path -Leaf $videoPath
    
    Write-Host "Video: $fileName" -ForegroundColor Cyan
    Write-Host "Path: $videoPath" -ForegroundColor Gray
    
    # Extract date from path
    if ($videoPath -match '(\d{4}-\d{2}-\d{2})') {
        $dateStr = $matches[1] -replace '-',''
        Write-Host "Date: $dateStr" -ForegroundColor Gray
    }
    
    # Extract time from filename
    if ($fileName -match '^(\d+)\.(\d+)\.(\d+)') {
        $hh = $matches[1].PadLeft(2, '0')
        $mm = $matches[2].PadLeft(2, '0')
        $ss = $matches[3].PadLeft(2, '0')
        $nmeaFileName = "NMEA_${dateStr}_${hh}${mm}${ss}.txt"
        Write-Host "NMEA: $nmeaFileName" -ForegroundColor Cyan
        
        # Check if NMEA exists
        $nmeaPath = "/storage/emulated/0/Android/data/com.dahua.nmea/files/NMEA/$nmeaFileName"
        Write-Host "Checking: $nmeaPath" -ForegroundColor Gray
        
        $checkResult = & $adb shell "test -f '$nmeaPath' && echo exists"
        if ($checkResult -match 'exists') {
            Write-Host "NMEA file exists on device!" -ForegroundColor Green
        } else {
            Write-Host "NMEA file not found on device" -ForegroundColor Red
        }
        
        Write-Host ""
        Write-Host "Downloading files..." -ForegroundColor Yellow
        
        # Download video
        $localVideo = Join-Path $targetDir $fileName
        Write-Host "  Video -> $localVideo"
        & $adb pull $videoPath $localVideo
        
        # Download NMEA if exists
        if ($checkResult -match 'exists') {
            $localNmea = Join-Path $targetDir "$([System.IO.Path]::GetFileNameWithoutExtension($fileName)).txt"
            Write-Host "  NMEA -> $localNmea"
            & $adb pull $nmeaPath $localNmea
        }
        
        Write-Host ""
        Write-Host "Download complete!" -ForegroundColor Green
        Write-Host "Files saved to: $targetDir" -ForegroundColor Cyan
    }
}

Write-Host ""
Write-Host "Press any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
