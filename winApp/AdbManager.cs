using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading.Tasks;

namespace DahuaNmeaViewer
{
    public class AdbManager
    {
        public string AdbPath { get; set; } = @"C:\Program Files (x86)\MPTManager\MPT\adb.exe";
        private const string DEVICE_VIDEO_PATH = "/storage/emulated/0/mpt/";
        private const string DEVICE_NMEA_PATH = "/storage/emulated/0/Android/data/com.dahua.nmea/files/NMEA/";

        public async Task<string> DownloadFilesAsync(string localDirectory, Action<string> progressCallback)
        {
            int videoCount = 0;
            int nmeaCount = 0;
            
            progressCallback?.Invoke("Checking device connection...");
            
            // Check device connection
            if (!await IsDeviceConnectedAsync())
            {
                throw new Exception("No device connected. Please connect device via USB and enable USB debugging.");
            }
            
            progressCallback?.Invoke("Finding files on device...");
            
            // Get list of video files
            var videoFiles = await GetDeviceVideoFilesAsync();
            
            if (videoFiles.Count == 0)
            {
                return "No video files found on device.";
            }
            
            progressCallback?.Invoke($"Found {videoFiles.Count} video(s). Downloading...");
            
            foreach (var videoFile in videoFiles)
            {
                try
                {
                    // Extract filename from full device path
                    var fileName = Path.GetFileName(videoFile);
                    progressCallback?.Invoke($"Processing: {fileName}");
                    
                    // Get NMEA filename based on video path
                    var nmeaFileName = ExtractNmeaFileName(videoFile);
                    
                    if (string.IsNullOrEmpty(nmeaFileName))
                    {
                        progressCallback?.Invoke($"  Warning: Cannot extract NMEA filename, skipping");
                        continue;
                    }
                    
                    progressCallback?.Invoke($"  Video: {fileName}");
                    progressCallback?.Invoke($"  NMEA: {nmeaFileName}");
                    
                    // Download video
                    var localVideoPath = Path.Combine(localDirectory, fileName);
                    progressCallback?.Invoke($"  Downloading video...");
                    await PullFileAsync(videoFile, localVideoPath);
                    videoCount++;
                    
                    // Download NMEA if exists
                    var nmeaDevicePath = $"{DEVICE_NMEA_PATH}{nmeaFileName}";
                    var localNmeaPath = Path.Combine(localDirectory, Path.GetFileNameWithoutExtension(fileName) + ".txt");
                    
                    progressCallback?.Invoke($"  Checking NMEA: {nmeaDevicePath}");
                    if (await FileExistsOnDeviceAsync(nmeaDevicePath))
                    {
                        progressCallback?.Invoke($"  Downloading NMEA...");
                        await PullFileAsync(nmeaDevicePath, localNmeaPath);
                        nmeaCount++;
                    }
                    else
                    {
                        progressCallback?.Invoke($"  NMEA not found on device");
                    }
                    
                    // Delete from device after successful download
                    progressCallback?.Invoke($"  Deleting from device...");
                    await DeleteFileFromDeviceAsync(videoFile);
                    
                    if (await FileExistsOnDeviceAsync(nmeaDevicePath))
                    {
                        await DeleteFileFromDeviceAsync(nmeaDevicePath);
                    }
                    
                    progressCallback?.Invoke($"  Complete!");
                }
                catch (Exception ex)
                {
                    var errFileName = Path.GetFileName(videoFile);
                    progressCallback?.Invoke($"ERROR: {errFileName} - {ex.Message}");
                }
            }
            
            return $"Download complete!\nVideos: {videoCount}\nNMEA files: {nmeaCount}\nFiles deleted from device.";
        }

        private string ExtractNmeaFileName(string videoFilePath)
        {
            // Convert DSJ filename to NMEA filename
            // Example: /storage/emulated/0/mpt/2026-01-15/001/dav/17/17.24.14-17.24.35[R][0@0][0].mp4
            //       -> NMEA_20260115_172414.txt
            try
            {
                // Extract just the filename from full path
                var fileName = Path.GetFileName(videoFilePath);
                
                // Extract time from filename (HH.MM.SS-...)
                var parts = fileName.Split('-')[0].Split('.');
                if (parts.Length >= 3)
                {
                    var hh = parts[0].PadLeft(2, '0');
                    var mm = parts[1].PadLeft(2, '0');
                    var ss = parts[2].PadLeft(2, '0');
                    
                    // Extract date from path (YYYY-MM-DD)
                    var pathParts = videoFilePath.Split('/');
                    string dateStr = DateTime.Now.ToString("yyyyMMdd");
                    
                    foreach (var part in pathParts)
                    {
                        // Look for date pattern YYYY-MM-DD
                        if (part.Length == 10 && part[4] == '-' && part[7] == '-')
                        {
                            dateStr = part.Replace("-", "");
                            break;
                        }
                    }
                    
                    return $"NMEA_{dateStr}_{hh}{mm}{ss}.txt";
                }
            }
            catch { }
            
            return "";
        }

        private async Task<bool> IsDeviceConnectedAsync()
        {
            var output = await RunAdbCommandAsync("devices");
            // Output format: "List of devices attached\nAM06FB3YAJ16469\tdevice"
            // Check if there's a line ending with "\tdevice" (tab + "device")
            var lines = output.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
            foreach (var line in lines)
            {
                var trimmed = line.Trim();
                // Skip header line
                if (trimmed.StartsWith("List of")) continue;
                // Check if line ends with "device" (not "devices")
                if (trimmed.EndsWith("device") && !trimmed.EndsWith("devices"))
                {
                    return true;
                }
            }
            return false;
        }

        private async Task<System.Collections.Generic.List<string>> GetDeviceVideoFilesAsync()
        {
            var files = new System.Collections.Generic.List<string>();
            
            // Use find command with proper escaping
            var output = await RunAdbCommandAsync($"shell \"find {DEVICE_VIDEO_PATH} -name '*.mp4' -type f 2>/dev/null\"");
            
            if (!string.IsNullOrWhiteSpace(output))
            {
                var lines = output.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
                foreach (var line in lines)
                {
                    var trimmed = line.Trim();
                    if (trimmed.EndsWith(".mp4") && trimmed.StartsWith("/"))
                    {
                        files.Add(trimmed);
                    }
                }
            }
            
            return files;
        }

        private async Task<bool> FileExistsOnDeviceAsync(string devicePath)
        {
            try
            {
                var output = await RunAdbCommandAsync($"shell \"test -f '{devicePath}' && echo exists\"");
                return output.Contains("exists");
            }
            catch
            {
                return false;
            }
        }

        private async Task PullFileAsync(string devicePath, string localPath)
        {
            await RunAdbCommandAsync($"pull \"{devicePath}\" \"{localPath}\"");
        }

        private async Task DeleteFileFromDeviceAsync(string devicePath)
        {
            await RunAdbCommandAsync($"shell rm \"{devicePath}\"");
        }

        private async Task<string> RunAdbCommandAsync(string arguments)
        {
            return await Task.Run(() =>
            {
                try
                {
                    var processInfo = new ProcessStartInfo
                    {
                        FileName = AdbPath,
                        Arguments = arguments,
                        UseShellExecute = false,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        CreateNoWindow = true,
                        StandardOutputEncoding = Encoding.UTF8
                    };

                    using var process = Process.Start(processInfo);
                    if (process == null)
                        throw new Exception("Failed to start ADB process");

                    var output = process.StandardOutput.ReadToEnd();
                    var error = process.StandardError.ReadToEnd();
                    
                    process.WaitForExit();

                    if (process.ExitCode != 0 && !string.IsNullOrEmpty(error))
                    {
                        throw new Exception($"ADB error: {error}");
                    }

                    return output;
                }
                catch (Exception ex)
                {
                    throw new Exception($"ADB command failed: {ex.Message}");
                }
            });
        }
    }
}
