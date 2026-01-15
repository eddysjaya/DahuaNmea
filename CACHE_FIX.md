# Fix Gradle Cache Corruption

## Problem
Error: `Could not read workspace metadata from C:\Users\eddys\.gradle\caches\8.9\transforms\...\metadata.bin`

Gradle cache is corrupted and Android Studio is locking the files.

## Solution (Easy Way - Recommended)

### Method 1: Invalidate Caches in Android Studio
1. **Close ALL Android Studio projects** (File → Close Project)
2. Wait for all processes to stop
3. **Reopen the project**: File → Open → `d:\development\DahuaNmea`
4. Go to: **File → Invalidate Caches / Restart...**
5. Click **"Invalidate and Restart"**
6. Wait for Android Studio to restart and re-index
7. Let Gradle sync automatically

This will:
- Clear corrupted Gradle cache
- Re-download all dependencies
- Fix metadata.bin issues
- Rebuild the project index

### Method 2: Manual Cache Clear (if Method 1 fails)

1. **Exit Android Studio completely**
2. **Kill all Java/Gradle processes**:
   ```powershell
   Get-Process | Where-Object {$_.ProcessName -like "*java*"} | Stop-Process -Force
   ```

3. **Delete Gradle cache**:
   ```powershell
   Remove-Item -Path "$env:USERPROFILE\.gradle\caches" -Recurse -Force
   Remove-Item -Path "$env:USERPROFILE\.gradle\daemon" -Recurse -Force
   ```

4. **Delete project cache**:
   ```powershell
   cd d:\development\DahuaNmea
   Remove-Item -Path ".gradle" -Recurse -Force
   Remove-Item -Path "build" -Recurse -Force
   Remove-Item -Path "app\build" -Recurse -Force
   ```

5. **Restart Android Studio** and open the project
6. Let Gradle sync (will re-download everything)

## Why This Happened

- Gradle daemon crashed or was terminated during build
- Network interruption during dependency download
- Disk I/O error during cache write
- Multiple Gradle processes accessing cache simultaneously

## Verification

After fix, check build works:
```bash
cd d:\development\DahuaNmea
.\gradlew.bat clean
.\gradlew.bat build
```

## Current Status

- ✅ Gradle 8.9 configured
- ✅ AGP 8.2.2 installed
- ✅ Kotlin 1.9.22 configured
- ❌ Cache corrupted (needs fix above)
- ❌ gradle-wrapper.jar missing (will auto-download on sync)

---

**Quick Fix**: Close Android Studio → Reopen project → File → Invalidate Caches / Restart
