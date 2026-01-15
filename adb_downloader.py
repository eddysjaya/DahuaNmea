#!/usr/bin/env python3
"""
ADB File Downloader for Dahua NMEA App
Downloads video and NMEA files from Android device via ADB
"""

import subprocess
import os
import sys
from datetime import datetime
from pathlib import Path

# ADB path
ADB_PATH = r"C:\Program Files (x86)\MPTManager\MPT\adb.exe"

# Remote paths on Android device
REMOTE_VIDEO_PATH = "/storage/emulated/0/Android/data/com.dahua.nmea/files/Videos/"
REMOTE_NMEA_PATH = "/storage/emulated/0/Android/data/com.dahua.nmea/files/Nmea/"

# Local save folders
LOCAL_VIDEO_FOLDER = "downloaded_videos"
LOCAL_NMEA_FOLDER = "downloaded_nmea"


def run_adb_command(command):
    """Execute ADB command and return output"""
    try:
        result = subprocess.run(
            [ADB_PATH] + command,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"Error: {e.stderr}")
        return None


def check_device():
    """Check if device is connected"""
    print("Checking device connection...")
    output = run_adb_command(["devices"])
    if output and "device" in output and output.count("device") > 1:
        print("✓ Device connected")
        return True
    else:
        print("✗ No device found. Please connect device via USB.")
        return False


def list_remote_files(remote_path):
    """List files in remote directory"""
    output = run_adb_command(["shell", "ls", "-1", remote_path])
    if output:
        files = [f.strip() for f in output.strip().split('\n') if f.strip()]
        return files
    return []


def get_file_size(remote_path):
    """Get file size in human-readable format"""
    output = run_adb_command(["shell", "ls", "-lh", remote_path])
    if output:
        parts = output.split()
        if len(parts) >= 5:
            return parts[4]
    return "unknown"


def download_file(remote_file, local_folder):
    """Download a single file from device"""
    local_path = os.path.join(local_folder, os.path.basename(remote_file))
    
    # Skip if file already exists
    if os.path.exists(local_path):
        print(f"  [SKIP] {os.path.basename(remote_file)} (already exists)")
        return False
    
    # Download file
    size = get_file_size(remote_file)
    print(f"  [DOWN] {os.path.basename(remote_file)} ({size})...", end=" ")
    
    result = subprocess.run(
        [ADB_PATH, "pull", remote_file, local_path],
        capture_output=True,
        text=True
    )
    
    if result.returncode == 0:
        print("✓")
        return True
    else:
        print("✗")
        print(f"    Error: {result.stderr}")
        return False


def download_all_files():
    """Download all video and NMEA files"""
    # Create local folders
    os.makedirs(LOCAL_VIDEO_FOLDER, exist_ok=True)
    os.makedirs(LOCAL_NMEA_FOLDER, exist_ok=True)
    
    print("\n" + "=" * 60)
    print("Dahua NMEA File Downloader")
    print("=" * 60)
    print(f"Video folder: {os.path.abspath(LOCAL_VIDEO_FOLDER)}")
    print(f"NMEA folder:  {os.path.abspath(LOCAL_NMEA_FOLDER)}")
    print("=" * 60)
    print()
    
    # Check device connection
    if not check_device():
        return
    
    print()
    
    # Download videos
    print("Downloading video files...")
    video_files = list_remote_files(REMOTE_VIDEO_PATH)
    video_files = [f for f in video_files if f.endswith('.mp4')]
    
    if video_files:
        downloaded_videos = 0
        for video_file in video_files:
            remote_file = REMOTE_VIDEO_PATH + video_file
            if download_file(remote_file, LOCAL_VIDEO_FOLDER):
                downloaded_videos += 1
        print(f"  Total: {downloaded_videos} new file(s) downloaded\n")
    else:
        print("  No video files found\n")
    
    # Download NMEA files
    print("Downloading NMEA files...")
    nmea_files = list_remote_files(REMOTE_NMEA_PATH)
    nmea_files = [f for f in nmea_files if f.endswith('.txt')]
    
    if nmea_files:
        downloaded_nmea = 0
        for nmea_file in nmea_files:
            remote_file = REMOTE_NMEA_PATH + nmea_file
            if download_file(remote_file, LOCAL_NMEA_FOLDER):
                downloaded_nmea += 1
        print(f"  Total: {downloaded_nmea} new file(s) downloaded\n")
    else:
        print("  No NMEA files found\n")
    
    print("=" * 60)
    print("Download complete!")
    print("=" * 60)


def show_device_files():
    """Show list of files on device without downloading"""
    if not check_device():
        return
    
    print("\n" + "=" * 60)
    print("Files on Device")
    print("=" * 60)
    
    # List videos
    print("\nVideo files:")
    output = run_adb_command(["shell", "ls", "-lh", REMOTE_VIDEO_PATH])
    if output:
        for line in output.strip().split('\n'):
            if '.mp4' in line:
                print(f"  {line}")
    
    # List NMEA
    print("\nNMEA files:")
    output = run_adb_command(["shell", "ls", "-lh", REMOTE_NMEA_PATH])
    if output:
        for line in output.strip().split('\n'):
            if '.txt' in line:
                print(f"  {line}")
    
    print("\n" + "=" * 60)


def main():
    """Main function"""
    if len(sys.argv) > 1 and sys.argv[1] == "list":
        show_device_files()
    else:
        download_all_files()


if __name__ == "__main__":
    main()
