# Debug ADB dalam aplikasi
$adb = "C:\Program Files (x86)\MPTManager\MPT\adb.exe"

Write-Host "=== ADB Debug Test ===" -ForegroundColor Cyan
Write-Host ""

# Test 1: File exists
Write-Host "1. Checking ADB file..." -ForegroundColor Yellow
if (Test-Path $adb) {
    Write-Host "   ✓ ADB exists: $adb" -ForegroundColor Green
} else {
    Write-Host "   ✗ ADB NOT FOUND: $adb" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Test 2: Can execute
Write-Host "2. Testing ADB execution..." -ForegroundColor Yellow
try {
    $version = & $adb version 2>&1
    Write-Host "   ✓ ADB can execute" -ForegroundColor Green
    Write-Host "   Version: $($version[0])" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Cannot execute ADB: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Test 3: Device connection
Write-Host "3. Checking device connection..." -ForegroundColor Yellow
$devices = & $adb devices 2>&1
if ($devices -match "device$") {
    Write-Host "   ✓ Device connected" -ForegroundColor Green
    $devices | ForEach-Object { if ($_ -match "device$") { Write-Host "   Device: $_" -ForegroundColor Gray } }
} else {
    Write-Host "   ✗ No device connected" -ForegroundColor Red
    Write-Host "   Output: $devices" -ForegroundColor Gray
}
Write-Host ""

# Test 4: Shell command
Write-Host "4. Testing shell command..." -ForegroundColor Yellow
try {
    $result = & $adb shell "echo test" 2>&1
    if ($result -match "test") {
        Write-Host "   ✓ Shell command works" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Shell command failed: $result" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Shell command error: $_" -ForegroundColor Red
}
Write-Host ""

# Test 5: Find command with proper escaping
Write-Host "5. Testing find command (with quotes)..." -ForegroundColor Yellow
try {
    $findCmd = 'shell "find /storage/emulated/0/mpt/ -name ''*.mp4'' -type f 2>/dev/null"'
    Write-Host "   Command: adb $findCmd" -ForegroundColor Gray
    
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $adb
    $psi.Arguments = $findCmd
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true
    
    $process = [System.Diagnostics.Process]::Start($psi)
    $output = $process.StandardOutput.ReadToEnd()
    $error = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    
    if ($output -match "\.mp4") {
        Write-Host "   OK Find command works" -ForegroundColor Green
        $lines = $output -split "`n" | Where-Object { $_ -match "\.mp4" }
        Write-Host "   Found $($lines.Count) video(s)" -ForegroundColor Gray
        $lines | Select-Object -First 2 | ForEach-Object { Write-Host "   - $_" -ForegroundColor Gray }
    } else {
        Write-Host "   WARNING No videos found" -ForegroundColor Yellow
        if ($error) {
            Write-Host "   Error: $error" -ForegroundColor Red
        }
        if ($output) {
            Write-Host "   Output: $output" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "   ERROR Find command exception: $_" -ForegroundColor Red
}
Write-Host ""

Write-Host "=== Test Complete ===" -ForegroundColor Cyan
