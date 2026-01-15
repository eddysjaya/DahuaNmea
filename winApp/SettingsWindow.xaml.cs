using System;
using System.Windows;
using Microsoft.Win32;
using System.Threading.Tasks;

namespace DahuaNmeaViewer
{
    public partial class SettingsWindow : Window
    {
        private AdbManager adbManager;

        public SettingsWindow(AdbManager manager)
        {
            InitializeComponent();
            adbManager = manager;
            LoadSettings();
        }

        private void LoadSettings()
        {
            txtAdbPath.Text = adbManager.AdbPath;
        }

        private void BtnBrowseAdb_Click(object sender, RoutedEventArgs e)
        {
            var dialog = new OpenFileDialog
            {
                Title = "Select ADB executable",
                Filter = "Executable files (*.exe)|*.exe|All files (*.*)|*.*",
                FileName = "adb.exe"
            };

            if (dialog.ShowDialog() == true)
            {
                txtAdbPath.Text = dialog.FileName;
            }
        }

        private async void BtnTestConnection_Click(object sender, RoutedEventArgs e)
        {
            var btn = (System.Windows.Controls.Button)sender;
            btn.IsEnabled = false;
            btn.Content = "Testing...";

            try
            {
                adbManager.AdbPath = txtAdbPath.Text;
                var result = await adbManager.DownloadFilesAsync("", (msg) => { });
                
                MessageBox.Show("Connection successful! Device is connected.", "Success", 
                              MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Connection failed:\n{ex.Message}", "Error", 
                              MessageBoxButton.OK, MessageBoxImage.Error);
            }
            finally
            {
                btn.IsEnabled = true;
                btn.Content = "Test Connection";
            }
        }

        private void BtnSave_Click(object sender, RoutedEventArgs e)
        {
            adbManager.AdbPath = txtAdbPath.Text;
            
            // Save to config file
            SaveSettings();
            
            DialogResult = true;
            Close();
        }

        private void BtnCancel_Click(object sender, RoutedEventArgs e)
        {
            DialogResult = false;
            Close();
        }

        private void SaveSettings()
        {
            try
            {
                var config = System.IO.Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                    "DahuaNmeaViewer", "config.txt");
                
                System.IO.Directory.CreateDirectory(System.IO.Path.GetDirectoryName(config)!);
                System.IO.File.WriteAllText(config, txtAdbPath.Text);
            }
            catch { }
        }
    }
}
