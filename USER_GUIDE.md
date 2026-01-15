# Panduan Penggunaan Dahua NMEA Recorder

## Memulai Aplikasi

### 1. Buka Aplikasi
- Tap icon "Dahua NMEA Recorder" di home screen
- Tunggu aplikasi loading
- Camera preview akan muncul

### 2. Cek Status Awal
- **Recording Indicator**: Bulat abu-abu = Ready
- **GPS Status**: "GPS: Searching..." = Mencari sinyal
- **Recording Time**: 00:00:00
- **GPS Points**: 0 pts

## Melakukan Perekaman

### Memulai Recording

1. **Persiapan**
   - Pastikan berada di area terbuka (untuk GPS signal)
   - Arahkan kamera ke objek yang akan direkam
   - Tunggu GPS status "GPS: Ready" (optimal)

2. **Tekan Tombol START**
   - Tombol START akan disabled
   - Tombol STOP akan enabled
   - Recording indicator berubah merah
   - Status: "Recording..."
   - Timer mulai berjalan
   - GPS points mulai bertambah

3. **Selama Recording**
   - Kamera akan merekam video HD
   - GPS tracking berjalan setiap 1 detik
   - Timer menampilkan durasi recording
   - GPS counter menampilkan jumlah titik GPS

### Menghentikan Recording

1. **Tekan Tombol STOP**
   - Recording akan berhenti
   - Status: "Stopping..."
   - Video dan NMEA file disimpan
   - Toast notification: "Recording stopped"

2. **Setelah Stop**
   - Tombol START enabled kembali
   - Tombol STOP disabled
   - Recording indicator abu-abu
   - Timer reset ke 00:00:00
   - GPS counter reset ke 0 pts
   - Files tersimpan di storage

## Memahami Interface

### Status Indikator

#### Recording Status
- 🔘 **Gray dot** = Ready (siap merekam)
- 🔴 **Red dot** = Recording (sedang merekam)
- **"Ready"** = Aplikasi siap
- **"Recording..."** = Sedang merekam
- **"Stopped"** = Perekaman selesai

#### GPS Status
- 📍 **"GPS: Searching..."** = Mencari sinyal GPS (tunggu)
- 📍 **"GPS: Ready"** = GPS lock (siap mulai)
- 📍 **"GPS: Active"** = Tracking aktif (recording)
- 📍 **"GPS: Error"** = GPS bermasalah

#### Timer Display
- Format: HH:MM:SS (Hours:Minutes:Seconds)
- Update setiap detik
- Max: 99:59:59

#### GPS Counter
- Format: "X pts" (X = jumlah titik GPS)
- Update setiap kali dapat lokasi baru
- 1 point per detik (normal)

### Tombol Kontrol

#### START Button
- **Warna**: Hijau
- **Fungsi**: Memulai recording
- **Enabled**: Saat ready (tidak recording)
- **Disabled**: Saat recording

#### STOP Button
- **Warna**: Merah
- **Fungsi**: Menghentikan recording
- **Enabled**: Saat recording
- **Disabled**: Saat ready (tidak recording)

## Tips Recording

### GPS Signal
✅ **GOOD**:
- Outdoor / area terbuka
- Langit terlihat jelas
- Tidak ada gedung tinggi
- Cold start: tunggu 30-60 detik
- Warm start: tunggu 5-15 detik

❌ **BAD**:
- Indoor / dalam ruangan
- Basement / garasi tertutup
- Dibawah pohon lebat
- Cuaca buruk (hujan lebat)

### Recording Quality

**Optimal Conditions**:
- GPS accuracy < 10m
- Recording di area terang
- Kamera tidak goyang
- Durasi cukup (> 30 detik)

**Tips**:
- Stabilkan perangkat saat recording
- Hindari recording saat jalan cepat
- Pastikan lensa bersih
- Check battery level

## Mengakses File Recording

### Lokasi File

Files tersimpan di:
```
Internal Storage/
└── Android/
    └── data/
        └── com.dahua.nmea/
            └── files/
                └── DahuaNmea/
                    ├── Videos/
                    │   └── VID_20260114_123456.mp4
                    └── NMEA/
                        └── GPS_20260114_123456.nmea
```

### Format Nama File

**Video**:
- Format: `VID_YYYYMMDD_HHMMSS.mp4`
- Contoh: `VID_20260114_153045.mp4`
- Artinya: Video tanggal 14 Jan 2026, jam 15:30:45

**NMEA**:
- Format: `GPS_YYYYMMDD_HHMMSS.nmea`
- Contoh: `GPS_20260114_153045.nmea`
- Artinya: GPS data tanggal 14 Jan 2026, jam 15:30:45

### Membuka File

**Video (.mp4)**:
- Buka dengan Video Player
- Supported by most players
- Copy ke PC untuk editing

**NMEA (.nmea)**:
- Text file format
- Buka dengan Text Editor
- Atau GPS analysis software

## Transfer File ke PC

### Method 1: USB File Transfer

1. **Connect Device**
   - Hubungkan perangkat ke PC via USB cable
   - Tunggu PC detect device

2. **Enable File Transfer Mode**
   - Swipe down notification panel
   - Tap "USB charging this device"
   - Pilih "File Transfer" atau "MTP"

3. **Browse Files di PC**
   - Windows: Buka "This PC" atau "My Computer"
   - Find device: "Dahua MPT230" atau nama device
   - Navigate ke: `Internal Storage/Android/data/com.dahua.nmea/files/DahuaNmea/`

4. **Copy Files**
   - Drag & drop folder "Videos" ke PC
   - Drag & drop folder "NMEA" ke PC
   - Atau copy individual files

### Method 2: Share via Apps

*Coming soon - will be implemented in future version*

### Method 3: ADB Pull

```bash
# Pull semua video files
adb pull /sdcard/Android/data/com.dahua.nmea/files/DahuaNmea/Videos/ ./videos/

# Pull semua NMEA files
adb pull /sdcard/Android/data/com.dahua.nmea/files/DahuaNmea/NMEA/ ./nmea/

# Pull specific file
adb pull "/sdcard/Android/data/com.dahua.nmea/files/DahuaNmea/Videos/VID_20260114_153045.mp4"
```

## Analisis Data NMEA

### Format NMEA

File NMEA berisi GPS sentences:

```
$GPRMC,153045.000,A,0630.1234,S,10645.6789,E,0.0,0.0,140126,,*6A
$GPGGA,153045.000,0630.1234,S,10645.6789,E,1,08,1.0,100.0,M,0.0,M,,*52
```

### Penjelasan GPRMC
```
$GPRMC,
  153045.000,    # Time: 15:30:45
  A,             # Status: A=Active (valid), V=Void (invalid)
  0630.1234,S,   # Latitude: 06°30.1234' South
  10645.6789,E,  # Longitude: 106°45.6789' East
  0.0,           # Speed (knots)
  0.0,           # Track angle (degrees)
  140126,        # Date: 14 Jan 2026
  ,              # Magnetic variation
  ,              # Mode indicator
*6A              # Checksum
```

### Penjelasan GPGGA
```
$GPGGA,
  153045.000,    # Time: 15:30:45
  0630.1234,S,   # Latitude: 06°30.1234' South
  10645.6789,E,  # Longitude: 106°45.6789' East
  1,             # Fix quality: 0=Invalid, 1=GPS, 2=DGPS
  08,            # Number of satellites
  1.0,           # HDOP (Horizontal Dilution of Precision)
  100.0,M,       # Altitude in meters
  0.0,M,         # Geoidal separation
  ,              # Age of differential GPS data
  ,              # Differential reference station ID
*52              # Checksum
```

### Software Analisis

**Recommended Tools**:
- **Google Earth**: Import GPX (convert NMEA to GPX)
- **GPSBabel**: Convert NMEA to various formats
- **QGIS**: GIS analysis
- **GPS Visualizer**: Online tool
- **Custom scripts**: Python, etc.

### Convert NMEA to GPX

Gunakan GPSBabel:
```bash
gpsbabel -i nmea -f GPS_20260114_153045.nmea -o gpx -F output.gpx
```

## Troubleshooting

### GPS Tidak Lock
**Problem**: GPS status stuck di "Searching..."

**Solutions**:
- ✅ Pindah ke area terbuka
- ✅ Tunggu 1-2 menit (cold start)
- ✅ Restart aplikasi
- ✅ Check Location settings di Android
- ✅ Enable "High Accuracy" mode

### Video Tidak Terekam
**Problem**: File video tidak tersimpan

**Solutions**:
- ✅ Check storage space (min 1GB free)
- ✅ Grant Camera permission
- ✅ Grant Audio permission
- ✅ Restart aplikasi
- ✅ Restart device

### Recording Tiba-tiba Stop
**Problem**: Recording berhenti sendiri

**Solutions**:
- ✅ Check battery level (min 20%)
- ✅ Disable battery optimization untuk app
- ✅ Close other apps
- ✅ Clear app cache

### File Tidak Bisa Transfer
**Problem**: Tidak bisa copy file ke PC

**Solutions**:
- ✅ Check USB cable (harus data cable)
- ✅ Select "File Transfer" mode
- ✅ Install device driver di PC
- ✅ Try different USB port
- ✅ Use ADB method

### NMEA File Kosong
**Problem**: File .nmea ada tapi kosong/sedikit data

**Solutions**:
- ✅ Pastikan GPS lock sebelum START
- ✅ Recording minimal 30 detik
- ✅ Check GPS permissions
- ✅ Test di area outdoor

## Best Practices

### Sebelum Recording
1. ✅ Check battery > 20%
2. ✅ Check storage > 500MB
3. ✅ Bersihkan lensa kamera
4. ✅ Pindah ke area GPS signal bagus
5. ✅ Tunggu GPS ready

### Selama Recording
1. ✅ Stabilkan perangkat
2. ✅ Jangan block GPS antenna
3. ✅ Hindari area signal buruk
4. ✅ Monitor GPS points count
5. ✅ Recording min 30 detik

### Setelah Recording
1. ✅ Tunggu sampai status "Stopped"
2. ✅ Verify files exist
3. ✅ Transfer ke PC regularly
4. ✅ Backup data penting
5. ✅ Clear old files if storage low

## FAQ

**Q: Berapa lama battery bertahan saat recording?**
A: Tergantung battery capacity. Estimasi 2-3 jam continuous recording.

**Q: Berapa ukuran file per menit?**
A: Video: ~10-15 MB/menit. NMEA: ~5 KB/menit.

**Q: Bisa recording tanpa GPS?**
A: Bisa, tapi file NMEA akan kosong/minimal data.

**Q: Maksimal durasi recording?**
A: Tergantung storage. 1GB ≈ 70 menit video.

**Q: Format video support PC?**
A: Ya, MP4 H.264 support semua player.

**Q: Bisa edit video di device?**
A: Tidak built-in, perlu app editor terpisah.

**Q: NMEA accuracy berapa meter?**
A: Tergantung GPS signal. Normal: 5-10 meter.

## Kontak Support

Untuk bantuan lebih lanjut, hubungi:
- **Dahua Technology Support**
- Email: support@dahuatech.com
- Website: www.dahuatech.com
