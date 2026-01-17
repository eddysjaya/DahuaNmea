using System;
using System.Collections.ObjectModel;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;
using Microsoft.Web.WebView2.Core;

namespace DahuaNmeaViewer
{
    public partial class MainWindow : Window
    {
        private ObservableCollection<SessionItem> sessions = new ObservableCollection<SessionItem>();
        private DispatcherTimer videoTimer;
        private bool isVideoPlaying = false;
        private bool isUserDraggingSlider = false;
        private SessionItem? currentSession;
        private AdbManager adbManager;
        private string dataDirectory;

        public MainWindow()
        {
            InitializeComponent();
            
            // Use Data folder in application directory
            var appDir = AppDomain.CurrentDomain.BaseDirectory;
            dataDirectory = Path.Combine(appDir, "Data");
            Directory.CreateDirectory(dataDirectory);
            
            adbManager = new AdbManager();
            lstSessions.ItemsSource = sessions;
            
            videoTimer = new DispatcherTimer();
            videoTimer.Interval = TimeSpan.FromMilliseconds(100);
            videoTimer.Tick += VideoTimer_Tick;
            
            InitializeAsync();
        }

        private async void InitializeAsync()
        {
            try
            {
                await mapWebView.EnsureCoreWebView2Async();
                await tableWebView.EnsureCoreWebView2Async();
                
                // Enable console message logging for debugging
                mapWebView.CoreWebView2.Settings.AreDevToolsEnabled = true;
                tableWebView.CoreWebView2.Settings.AreDevToolsEnabled = true;
                
                // Add console message handler to see errors
                mapWebView.CoreWebView2.WebMessageReceived += (s, e) =>
                {
                    MessageBox.Show($"WebView Message: {e.WebMessageAsJson}", "Debug");
                };
                
                LoadSessions();
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Initialization error: {ex.Message}", "Error", 
                              MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private void LoadSessions()
        {
            sessions.Clear();
            txtStatusBar.Text = "Loading sessions...";
            
            if (!Directory.Exists(dataDirectory))
            {
                txtStatusBar.Text = "No sessions found";
                return;
            }

            var videoFiles = Directory.GetFiles(dataDirectory, "*.mp4", SearchOption.AllDirectories);
            
            foreach (var videoPath in videoFiles)
            {
                var fileName = Path.GetFileNameWithoutExtension(videoPath);
                var nmeaPath = Path.Combine(Path.GetDirectoryName(videoPath)!, fileName + ".txt");
                
                if (File.Exists(nmeaPath))
                {
                    var fileInfo = new FileInfo(videoPath);
                    var session = new SessionItem
                    {
                        VideoPath = videoPath,
                        NmeaPath = nmeaPath,
                        DisplayName = fileName,
                        TimeInfo = fileInfo.LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss"),
                        SizeInfo = $"{fileInfo.Length / (1024 * 1024):F1} MB",
                        VideoFileName = Path.GetFileName(videoPath),
                        NmeaFileName = Path.GetFileName(nmeaPath)
                    };
                    sessions.Add(session);
                }
            }
            
            txtStatusBar.Text = $"Found {sessions.Count} session(s)";
        }

        private async void BtnDownload_Click(object sender, RoutedEventArgs e)
        {
            btnDownload.IsEnabled = false;
            progressPanel.Visibility = Visibility.Visible;
            downloadProgressBar.Value = 0;
            txtProgressPercent.Text = "0%";
            txtStatus.Text = "Starting download...";
            
            int totalFiles = 0;
            int currentFile = 0;
            
            try
            {
                txtStatus.Text = $"Using ADB: {adbManager.AdbPath}";
                await Task.Delay(500);
                
                txtStatus.Text = $"Target directory: {dataDirectory}";
                await Task.Delay(500);
                
                // Custom progress callback with file counting
                var result = await adbManager.DownloadFilesAsync(dataDirectory, 
                    (progress) => Dispatcher.Invoke(() => 
                    {
                        txtStatus.Text = progress;
                        
                        // Count total files on first "Found X video(s)" message
                        if (progress.Contains("Found") && progress.Contains("video"))
                        {
                            var parts = progress.Split(' ');
                            for (int i = 0; i < parts.Length; i++)
                            {
                                if (parts[i] == "Found" && i + 1 < parts.Length)
                                {
                                    if (int.TryParse(parts[i + 1], out int count))
                                    {
                                        totalFiles = count;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // Update progress bar when completing files
                        if (progress.Contains("Complete!") && totalFiles > 0)
                        {
                            currentFile++;
                            double percentage = ((double)currentFile / totalFiles) * 100;
                            downloadProgressBar.Value = percentage;
                            txtProgressPercent.Text = $"{percentage:F0}%";
                        }
                        
                        // Set to 100% when done
                        if (progress.Contains("Download complete"))
                        {
                            downloadProgressBar.Value = 100;
                            txtProgressPercent.Text = "100%";
                        }
                    }));
                
                downloadProgressBar.Value = 100;
                txtProgressPercent.Text = "100%";
                txtStatus.Text = "Download complete!";
                
                MessageBox.Show(result, "Download Complete", MessageBoxButton.OK, MessageBoxImage.Information);
                LoadSessions();
                
                // Hide progress bar after 2 seconds
                await Task.Delay(2000);
                progressPanel.Visibility = Visibility.Collapsed;
            }
            catch (Exception ex)
            {
                var errorMsg = $"Download failed!\n\nError: {ex.Message}\n\nStack Trace:\n{ex.StackTrace}";
                MessageBox.Show(errorMsg, "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                txtStatus.Text = $"Error: {ex.Message}";
                progressPanel.Visibility = Visibility.Collapsed;
            }
            finally
            {
                btnDownload.IsEnabled = true;
            }
        }

        private void BtnRefresh_Click(object sender, RoutedEventArgs e)
        {
            LoadSessions();
        }

        private void BtnSettings_Click(object sender, RoutedEventArgs e)
        {
            var settingsWindow = new SettingsWindow(adbManager);
            settingsWindow.Owner = this;
            settingsWindow.ShowDialog();
        }

        private void BtnClearAll_Click(object sender, RoutedEventArgs e)
        {
            var result = MessageBox.Show(
                $"Are you sure you want to delete all {sessions.Count} session(s)?\n\n" +
                "This will permanently delete all video and NMEA files!",
                "Clear All Sessions",
                MessageBoxButton.YesNo,
                MessageBoxImage.Warning);

            if (result == MessageBoxResult.Yes)
            {
                try
                {
                    int deletedCount = 0;
                    foreach (var session in sessions)
                    {
                        try
                        {
                            if (File.Exists(session.VideoPath))
                            {
                                File.Delete(session.VideoPath);
                                deletedCount++;
                            }
                            if (File.Exists(session.NmeaPath))
                            {
                                File.Delete(session.NmeaPath);
                            }
                        }
                        catch (Exception ex)
                        {
                            MessageBox.Show($"Error deleting {session.DisplayName}: {ex.Message}",
                                          "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                        }
                    }

                    MessageBox.Show($"Deleted {deletedCount} session(s) successfully!",
                                  "Clear Complete",
                                  MessageBoxButton.OK,
                                  MessageBoxImage.Information);

                    // Stop current playback
                    mediaPlayer.Stop();
                    mediaPlayer.Source = null;
                    currentSession = null;

                    // Refresh list
                    LoadSessions();
                }
                catch (Exception ex)
                {
                    MessageBox.Show($"Error clearing sessions: {ex.Message}",
                                  "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                }
            }
        }

        private async void LstSessions_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (lstSessions.SelectedItem is SessionItem session)
            {
                currentSession = session;
                await LoadSessionAsync(session);
            }
        }

        private async Task LoadSessionAsync(SessionItem session)
        {
            try
            {
                // Update file info display
                txtCurrentVideoFile.Text = session.VideoFileName ?? Path.GetFileName(session.VideoPath);
                txtCurrentNmeaFile.Text = session.NmeaFileName ?? Path.GetFileName(session.NmeaPath);
                
                // Load video
                mediaPlayer.Source = new Uri(session.VideoPath);
                txtVideoPlaceholder.Visibility = Visibility.Collapsed;
                
                // Load map
                if (File.Exists(session.NmeaPath))
                {
                    try
                    {
                        var gpsPoints = NmeaParser.ParseNmeaFile(session.NmeaPath);
                        if (gpsPoints.Count > 0)
                        {
                            await LoadMapAsync(gpsPoints);
                            txtGpsInfo.Text = $"📍 GPS Points: {gpsPoints.Count} | File: {session.NmeaFileName}";
                        }
                        else
                        {
                            txtMapPlaceholder.Text = "🗺 No GPS data in file";
                            txtMapPlaceholder.Visibility = Visibility.Visible;
                            txtGpsInfo.Text = "No GPS data in file";
                        }
                    }
                    catch (Exception mapEx)
                    {
                        txtMapPlaceholder.Text = $"Error loading GPS data: {mapEx.Message}";
                        txtMapPlaceholder.Visibility = Visibility.Visible;
                        MessageBox.Show($"GPS Parse Error: {mapEx.Message}", "Error", 
                                      MessageBoxButton.OK, MessageBoxImage.Warning);
                    }
                }
                else
                {
                    txtMapPlaceholder.Text = "🗺 No NMEA file found";
                    txtMapPlaceholder.Visibility = Visibility.Visible;
                    txtCurrentNmeaFile.Text = $"{Path.GetFileName(session.NmeaPath)} (NOT FOUND)";
                }
                
                txtStatusBar.Text = $"Loaded: {session.DisplayName}";
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Error loading session: {ex.Message}", "Error", 
                              MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private async Task LoadMapAsync(System.Collections.Generic.List<GpsPoint> points)
        {
            try
            {
                Console.WriteLine($"[LoadMap] Loading map with {points.Count} GPS points");
                
                // Ensure WebView2s are initialized
                if (mapWebView.CoreWebView2 == null)
                {
                    Console.WriteLine("[LoadMap] Initializing map WebView2...");
                    await mapWebView.EnsureCoreWebView2Async();
                }
                
                if (tableWebView.CoreWebView2 == null)
                {
                    Console.WriteLine("[LoadMap] Initializing table WebView2...");
                    await tableWebView.EnsureCoreWebView2Async();
                }
                
                // Generate map HTML
                var mapHtml = MapHtmlGenerator.GenerateHtml(points);
                var tempMapPath = Path.Combine(Path.GetTempPath(), $"dahua_map_{DateTime.Now.Ticks}.html");
                File.WriteAllText(tempMapPath, mapHtml);
                
                // Generate table HTML
                var tableHtml = MapHtmlGenerator.GenerateTableHtml(points);
                var tempTablePath = Path.Combine(Path.GetTempPath(), $"dahua_table_{DateTime.Now.Ticks}.html");
                File.WriteAllText(tempTablePath, tableHtml);
                
                Console.WriteLine($"[LoadMap] Generated map and table HTML, loading...");
                
                // Navigate to new file URLs (forces complete reload)
                mapWebView.Source = new Uri(tempMapPath);
                tableWebView.Source = new Uri(tempTablePath);
                
                txtMapPlaceholder.Visibility = Visibility.Collapsed;
                
                Console.WriteLine("[LoadMap] Map and table loaded successfully");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[LoadMap] ERROR: {ex.Message}");
                MessageBox.Show($"Error loading map: {ex.Message}\n\nStack: {ex.StackTrace}", 
                              "Map Error", MessageBoxButton.OK, MessageBoxImage.Error);
                txtMapPlaceholder.Text = $"Map Error: {ex.Message}";
                txtMapPlaceholder.Visibility = Visibility.Visible;
            }
        }

        private void MediaPlayer_MediaOpened(object sender, RoutedEventArgs e)
        {
            if (mediaPlayer.NaturalDuration.HasTimeSpan)
            {
                var duration = mediaPlayer.NaturalDuration.TimeSpan.TotalSeconds;
                sliderProgress.Maximum = duration;
                txtTotalTime.Text = FormatTime(mediaPlayer.NaturalDuration.TimeSpan);
                
                // Set default volume
                mediaPlayer.Volume = 1.0; // Full volume
                
                // Log for debugging
                System.Diagnostics.Debug.WriteLine($"=== VIDEO OPENED ===");
                System.Diagnostics.Debug.WriteLine($"Duration: {duration} seconds");
                System.Diagnostics.Debug.WriteLine($"Volume: {mediaPlayer.Volume}");
                System.Diagnostics.Debug.WriteLine($"HasAudio: {mediaPlayer.HasAudio}");
                
                // Send actual video duration to JavaScript for accurate animation
                SetVideoDuration(duration);
            }
        }

        private void MediaPlayer_MediaEnded(object sender, RoutedEventArgs e)
        {
            videoTimer.Stop();
            isVideoPlaying = false;
        }

        private void BtnPlay_Click(object sender, RoutedEventArgs e)
        {
            if (mediaPlayer.Source != null)
            {
                mediaPlayer.Play();
                videoTimer.Start();
                isVideoPlaying = true;
            }
        }

        private void BtnPause_Click(object sender, RoutedEventArgs e)
        {
            mediaPlayer.Pause();
            videoTimer.Stop();
            isVideoPlaying = false;
        }

        private void BtnStop_Click(object sender, RoutedEventArgs e)
        {
            mediaPlayer.Stop();
            videoTimer.Stop();
            isVideoPlaying = false;
            sliderProgress.Value = 0;
        }

        private void VideoTimer_Tick(object? sender, EventArgs e)
        {
            if (!isUserDraggingSlider && mediaPlayer.Source != null)
            {
                sliderProgress.Value = mediaPlayer.Position.TotalSeconds;
                txtCurrentTime.Text = FormatTime(mediaPlayer.Position);
                
                // Update map marker position
                if (currentSession != null && isVideoPlaying)
                {
                    UpdateMapMarker(mediaPlayer.Position.TotalSeconds);
                }
            }
        }

        private void SliderProgress_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (isUserDraggingSlider && mediaPlayer.Source != null)
            {
                mediaPlayer.Position = TimeSpan.FromSeconds(sliderProgress.Value);
            }
        }

        private async void UpdateMapMarker(double currentSeconds)
        {
            try
            {
                System.Diagnostics.Debug.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] UpdateMapMarker: {currentSeconds:F2}s");
                
                // Update map marker position
                var mapScript = $"updateMarkerPosition({currentSeconds.ToString(System.Globalization.CultureInfo.InvariantCulture)});";
                await mapWebView.CoreWebView2.ExecuteScriptAsync(mapScript);
                
                // Get current point index from map
                var indexResult = await mapWebView.CoreWebView2.ExecuteScriptAsync("window.currentDisplayIndex");
                if (int.TryParse(indexResult, out int pointIndex))
                {
                    // Update table highlight
                    var tableScript = $"highlightRow({pointIndex});";
                    await tableWebView.CoreWebView2.ExecuteScriptAsync(tableScript);
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"UpdateMapMarker error: {ex.Message}");
            }
        }

        private async void SetVideoDuration(double durationSeconds)
        {
            try
            {
                System.Diagnostics.Debug.WriteLine($"=== SETTING VIDEO DURATION ===");
                System.Diagnostics.Debug.WriteLine($"Duration: {durationSeconds} seconds");
                
                // Wait for map to be ready
                await Task.Delay(100);
                
                var script = $"if(typeof setVideoDuration === 'function') {{ setVideoDuration({durationSeconds.ToString(System.Globalization.CultureInfo.InvariantCulture)}); }} else {{ console.error('setVideoDuration not found'); }}";
                var result = await mapWebView.CoreWebView2.ExecuteScriptAsync(script);
                
                System.Diagnostics.Debug.WriteLine($"✅ Video duration sent to JavaScript: {result}");
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"❌ Error setting video duration: {ex.Message}");
            }
        }

        private string FormatTime(TimeSpan time)
        {
            return $"{(int)time.TotalMinutes:D2}:{time.Seconds:D2}";
        }

        protected override void OnClosed(EventArgs e)
        {
            videoTimer?.Stop();
            mediaPlayer?.Close();
            base.OnClosed(e);
        }
    }

    public class SessionItem
    {
        public string VideoPath { get; set; } = "";
        public string NmeaPath { get; set; } = "";
        public string DisplayName { get; set; } = "";
        public string TimeInfo { get; set; } = "";
        public string SizeInfo { get; set; } = "";
        public string VideoFileName { get; set; } = "";
        public string NmeaFileName { get; set; } = "";
    }
}
