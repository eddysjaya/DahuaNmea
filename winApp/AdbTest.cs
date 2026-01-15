using System;
using System.Diagnostics;
using System.Threading.Tasks;

namespace AdbTest
{
    class Program
    {
        static async Task Main(string[] args)
        {
            var adbPath = @"C:\Program Files (x86)\MPTManager\MPT\adb.exe";
            
            Console.WriteLine("=== ADB Test ===");
            Console.WriteLine($"ADB Path: {adbPath}");
            Console.WriteLine();
            
            // Test 1: Check devices
            Console.WriteLine("Test 1: Checking devices...");
            var devices = await RunAdbCommand(adbPath, "devices");
            Console.WriteLine(devices);
            Console.WriteLine();
            
            // Test 2: Find videos
            Console.WriteLine("Test 2: Finding videos...");
            var videos = await RunAdbCommand(adbPath, "shell \"find /storage/emulated/0/mpt/ -name '*.mp4' -type f 2>/dev/null\"");
            Console.WriteLine(videos);
            Console.WriteLine();
            
            Console.WriteLine("Press any key to exit...");
            Console.ReadKey();
        }
        
        static async Task<string> RunAdbCommand(string adbPath, string arguments)
        {
            return await Task.Run(() =>
            {
                try
                {
                    var psi = new ProcessStartInfo
                    {
                        FileName = adbPath,
                        Arguments = arguments,
                        UseShellExecute = false,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        CreateNoWindow = true
                    };
                    
                    using var process = Process.Start(psi);
                    if (process == null) return "ERROR: Failed to start process";
                    
                    var output = process.StandardOutput.ReadToEnd();
                    var error = process.StandardError.ReadToEnd();
                    process.WaitForExit();
                    
                    if (process.ExitCode != 0 && !string.IsNullOrEmpty(error))
                    {
                        return $"ERROR: {error}";
                    }
                    
                    return output;
                }
                catch (Exception ex)
                {
                    return $"EXCEPTION: {ex.Message}";
                }
            });
        }
    }
}
