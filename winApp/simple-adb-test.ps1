# Simple ADB Test
$adb = "C:\Program Files (x86)\MPTManager\MPT\adb.exe"

Write-Host "ADB Test" -ForegroundColor Cyan

# Test devices
Write-Host "Testing devices..." -ForegroundColor Yellow
& $adb devices
Write-Host ""

# Test find with ProcessStartInfo (sama seperti di C#)
Write-Host "Testing find command..." -ForegroundColor Yellow
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $adb
$psi.Arguments = 'shell "find /storage/emulated/0/mpt/ -name ''*.mp4'' -type f 2>/dev/null"'
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true

$process = [System.Diagnostics.Process]::Start($psi)
$output = $process.StandardOutput.ReadToEnd()
$error = $process.StandardError.ReadToEnd()
$process.WaitForExit()

Write-Host "Exit Code: $($process.ExitCode)"
Write-Host "Output:"
Write-Host $output
if ($error) {
    Write-Host "Error:"
    Write-Host $error -ForegroundColor Red
}
