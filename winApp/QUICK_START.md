# Dahua NMEA Viewer - Quick Start Guide

## 🚀 Cara Menjalankan

### Pertama Kali
```powershell
cd d:\development\DahuaNmea\winApp
dotnet restore
dotnet build
dotnet run
```

### Selanjutnya
```powershell
cd d:\development\DahuaNmea\winApp
dotnet run
```

Atau klik 2x file: `bin\Debug\net6.0-windows\DahuaNmeaViewer.exe`

---

## ⚙️ Konfigurasi ADB

1. Klik tombol **Settings** (ikon gear)
2. Atur path ADB:
   - Default: `C:\Program Files (x86)\MPTManager\MPT\adb.exe`
   - Atau cari dengan tombol **Browse**
3. Klik **Test Connection** untuk verifikasi
4. Klik **Save**

---

## 📱 Persiapan Device Android

1. **Enable USB Debugging**
   - Settings → About Phone → Tap "Build Number" 7x
   - Settings → Developer Options → Enable "USB Debugging"

2. **Hubungkan USB**
   - Colokkan device ke komputer
   - Accept "Allow USB Debugging" dialog

3. **Test Koneksi**
   ```powershell
   adb devices
   ```
   Harus muncul device dengan status "device"

---

## 📥 Download Files dari Device

1. Klik tombol **Download** (hijau) di toolbar
2. Aplikasi akan:
   - Mencari semua file `.mp4` di `/storage/emulated/0/mpt/`
   - Download video + NMEA matching (dari `/storage/emulated/0/Android/data/com.dahua.nmea/files/NMEA/`)
   - Hapus otomatis dari device setelah berhasil
3. File tersimpan di: `d:\development\DahuaNmea\winApp\Data\`

**Path di Device:**
- Video: `/storage/emulated/0/mpt/YYYY-MM-DD/001/dav/HH/*.mp4`
- NMEA: `/storage/emulated/0/Android/data/com.dahua.nmea/files/NMEA/NMEA_YYYYMMDD_HHMMSS.txt`

**Contoh:**
- Video: `/storage/emulated/0/mpt/2026-01-15/001/dav/17/17.24.14-17.24.35[R][0@0][0].mp4`
- NMEA: `/storage/emulated/0/Android/data/com.dahua.nmea/files/NMEA/NMEA_20260115_172414.txt`

---

## ▶️ Playback Video + GPS

1. Pilih session dari list di kiri
2. Video akan load otomatis
3. GPS track muncul di peta (kanan)
4. Klik **Play** untuk mulai
5. GPS marker (orange) akan mengikuti posisi video

**Kontrol:**
- Play/Pause/Stop buttons
- Slider untuk seek
- Volume control

---

## 🗺 Peta OpenStreetMap

**Marker Colors:**
- 🟢 **Green** = START (awal rekaman)
- 🔴 **Red** = END (akhir rekaman)
- 🟠 **Orange** = Current Position (posisi video sekarang)

**Track Line:**
- 🔵 **Blue** = Route yang sudah dilalui

---

## 📂 Struktur Folder

```
winApp\
├── Data\                    ← Video & NMEA disimpan disini
│   ├── 16.11.00-16.11.54[R][0@0][0].mp4
│   └── NMEA_20260115_161100.txt
├── bin\Debug\net6.0-windows\
│   └── DahuaNmeaViewer.exe  ← Executable
└── ...
```

---

## 🔧 Troubleshooting

### Aplikasi tidak buka
- Install .NET 6.0 Desktop Runtime
- Download: https://dotnet.microsoft.com/download/dotnet/6.0

### WebView2 error
- Install Microsoft Edge WebView2 Runtime
- Download: https://developer.microsoft.com/microsoft-edge/webview2/

### ADB tidak ketemu
- Install MPT Manager (sudah include ADB)
- Atau download Android SDK Platform Tools
- Update path di Settings

### Device tidak terdeteksi
- Pastikan USB Debugging enabled
- Coba cabut-colok USB
- Coba `adb kill-server` lalu `adb start-server`

### Video tidak ada
- Pastikan ada rekaman di device
- Path device: `/storage/emulated/0/mpt/YYYY-MM-DD/001/dav/HH/`
- Coba download manual via File Explorer

### GPS tidak muncul
- Pastikan file NMEA ada dengan nama matching
- Format: `NMEA_YYYYMMDD_HHMMSS.txt`
- Check isi file, harus ada baris `$GPGGA` atau `$GNGGA`

---

## 📋 Format File

### Video File
```
16.11.00-16.11.54[R][0@0][0].mp4
└─ HH.MM.SS-HH.MM.SS = time range
```

### NMEA File
```
NMEA_20260115_161100.txt
└─ NMEA_YYYYMMDD_HHMMSS = timestamp
```

**Isi NMEA:**
```
$GPGGA,161100.00,0619.8288,S,10647.6631,E,1,08,0.9,25.0,M,-32.0,M,,*5E
$GPGGA,161101.00,0619.8290,S,10647.6633,E,1,08,0.9,26.0,M,-32.0,M,,*5F
```

---

## 🎯 Workflow Lengkap

1. **Android App** (di device)
   - Auto-record video dengan DSJ Camera
   - Auto-tracking GPS di background
   - Generate NMEA file otomatis

2. **Transfer** (via USB)
   - Hubungkan device
   - Klik Download di Windows app
   - File auto-download + auto-delete

3. **Playback** (di Windows)
   - Pilih session
   - Play video
   - Lihat GPS track synchronized

---

## 💡 Tips

- Refresh list setelah download dengan tombol **Refresh**
- Video bisa di-seek dengan drag slider
- Zoom peta: scroll mouse di area map
- Pan peta: drag dengan mouse
- Marker otomatis center saat play dimulai

---

## 📞 Support

Jika ada masalah:
1. Check logcat: `adb logcat | grep DsjCamera`
2. Check build output di terminal
3. Pastikan semua file `.mp4` punya pasangan `.txt`

---

**Versi:** 1.0  
**Build:** .NET 6.0 Windows  
**Platform:** Windows 10/11 x64
