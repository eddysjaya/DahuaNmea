# Integrasi DSJ Camera dengan GPS Tracking Otomatis

## Fitur Baru

Aplikasi DahuaNmea sekarang dapat **otomatis track GPS** saat DSJ Camera merekam video! 
Video dari DSJ Camera dan file NMEA GPS akan memiliki **timestamp yang sama** untuk sinkronisasi sempurna.

## Cara Penggunaan

### 1. Aktifkan Monitor DSJ Camera

Di halaman utama aplikasi DahuaNmea:
- Lihat toggle switch "**DSJ Auto Track**" di kiri atas
- **Nyalakan** toggle switch tersebut
- Notifikasi akan muncul: "DSJ Camera monitoring started"
- Icon GPS akan muncul di notification bar

### 2. Rekam Video dengan DSJ Camera

Sekarang buka DSJ Camera (tekan tombol 📷 atau buka manual):
- Mulai recording video seperti biasa
- **Aplikasi DahuaNmea otomatis mendeteksi** video baru sedang direkam
- GPS tracking akan **otomatis dimulai**
- File NMEA dibuat dengan **timestamp yang match** dengan video

### 3. Stop Recording

- Stop recording di DSJ Camera
- GPS tracking **otomatis berhenti**
- File NMEA disimpan dengan nama: `NMEA_yyyyMMdd_HHmmss.txt`

### 4. File Hasil

Setelah recording, Anda akan memiliki:
```
Video DSJ: /storage/emulated/0/mpt/2026-01-15/001/dav/14/14.30.45-14.31.12[R][0@0][0].mp4
NMEA GPS: /storage/emulated/0/Android/data/com.dahua.nmea/files/NMEA/NMEA_20260115_143045.txt
```

**Timestamp match!** Video dimulai 14:30:45 = NMEA file 143045

### 5. Transfer Files

**PENTING**: Video DSJ Camera sekarang bisa ditransfer via WiFi/USB!

- Buka aplikasi DahuaNmea
- Tekan tombol "📁 Transfer"
- Sistem akan scan **2 lokasi**:
  - ✅ Video aplikasi kita: `/files/Videos/`
  - ✅ **Video DSJ Camera**: `/mpt/...`
- Pilih method transfer: WiFi, USB, atau ZIP
- Transfer via WiFi atau USB seperti biasa

#### Lokasi File

**Video DSJ Camera**:
```
/storage/emulated/0/mpt/2026-01-15/001/dav/14/14.30.45-14.31.12[R][0@0][0].mp4
```

**NMEA GPS** (auto-generated):
```
/storage/emulated/0/Android/data/com.dahua.nmea/files/NMEA/NMEA_20260115_143045.txt
```

**Saat transfer, KEDUA file akan dikirim**:
- Video DSJ: `.mp4` dari folder `/mpt/`
- NMEA GPS: `.txt` dari folder `/files/NMEA/`

#### Format Transfer

Saat transfer via WiFi/USB, sistem akan mengirim:
1. **Video DSJ Camera** (lokasi asli di `/mpt/`)
2. **File NMEA** yang matching (berdasarkan timestamp)

Contoh session yang ditransfer:
```
Session 1 (DSJ Camera):
├── 14.30.45-14.31.12[R][0@0][0].mp4   (27 MB - dari /mpt/)
└── NMEA_20260115_143045.txt           (15 KB - GPS tracking)

Session 2 (App):
├── VID_20260115_120354.mp4            (14 MB - dari /files/Videos/)
└── NMEA_20260115_120354.txt           (12 KB - GPS tracking)
```

## Keuntungan

✅ **Otomatis**: Tidak perlu manual start/stop tracking  
✅ **Sinkron**: Timestamp video dan NMEA selalu match  
✅ **Background**: Berjalan di background, tidak mengganggu  
✅ **Efisien**: GPS hanya aktif saat DSJ Camera merekam  
✅ **1 Session**: Setiap video DSJ = 1 file NMEA GPS  

## Status Monitoring

Aplikasi menampilkan status di notification:
- **"Monitoring DSJ Camera..."** → Standby, menunggu recording
- **"Tracking: NMEA_20260115_143045.txt"** → Sedang tracking GPS untuk video aktif

## Lokasi Video DSJ

DSJ Camera menyimpan video di:
```
/storage/emulated/0/mpt/YYYY-MM-DD/001/dav/HH/HH.MM.SS-HH.MM.SS[R][0@0][0].mp4
```

Contoh struktur folder:
```
/storage/emulated/0/mpt/
├── 2026-01-15/
│   └── 001/
│       └── dav/
│           ├── 08/
│           │   ├── 08.58.31-08.58.41[R][0@0][0].mp4
│           │   └── 08.58.47-08.59.11[R][0@0][0].mp4
│           ├── 14/
│           │   ├── 14.26.50-14.26.54[R][0@0][0].mp4
│           │   └── 14.30.45-14.31.12[R][0@0][0].mp4
│           └── ...
```

## Ekstraksi Timestamp

Aplikasi otomatis ekstrak timestamp dari nama file DSJ:
- **Format DSJ**: `HH.MM.SS-HH.MM.SS[R][0@0][0].mp4`
- **Contoh**: `14.30.45-14.31.12[R][0@0][0].mp4`
- **Timestamp awal**: 14.30.45 (14:30:45)
- **File NMEA**: `NMEA_20260115_143045.txt`

## Format NMEA

File NMEA menggunakan standard NMEA 0183:
```
$GPGGA,143045.000,0123.4567,S,10654.3210,E,1,08,1.0,25.0,M,0.0,M,,*XX
$GPGGA,143046.000,0123.4568,S,10654.3211,E,1,08,1.0,25.1,M,0.0,M,,*XX
...
```

Setiap baris = 1 GPS point per detik.

## Tips

1. **Selalu aktifkan** "DSJ Auto Track" sebelum merekam dengan DSJ Camera
2. **GPS harus enabled** di system settings
3. **Tunggu GPS fix** (biasanya 10-30 detik di outdoor)
4. **Notifikasi persistent** menunjukkan service aktif
5. **Matikan** toggle saat tidak diperlukan untuk hemat battery

## Troubleshooting

**Q: Toggle switch tidak bisa diaktifkan**  
A: Pastikan permission lokasi sudah granted

**Q: GPS tidak tracking meskipun DSJ recording**  
A: Periksa GPS enabled di system settings, coba restart device

**Q: File NMEA tidak match dengan video DSJ**  
A: Cek apakah toggle "DSJ Auto Track" aktif SEBELUM start recording

**Q: Notifikasi "DSJ path not found"**  
A: Folder `/storage/emulated/0/mpt` tidak ada, rekam 1 video di DSJ Camera dulu

## Technical Details

- **Monitoring**: FileObserver pada `/storage/emulated/0/mpt`
- **Events**: CREATE (start recording) → CLOSE_WRITE (stop recording)
- **GPS**: LocationManager dengan 1 second interval
- **Format**: NMEA 0183 GPGGA sentences
- **Service**: Foreground service dengan notification

## Perbandingan Mode

| Mode | Kontrol | Video Path | NMEA Path | Use Case |
|------|---------|-----------|-----------|----------|
| **Manual (Aplikasi Kita)** | Manual start/stop | `/files/Videos/` | `/files/NMEA/` | Simple tracking, control penuh |
| **Auto (DSJ Integration)** | Otomatis dengan DSJ | `/mpt/...` (DSJ) | `/files/NMEA/` | Professional video + GPS tracking |

## Status: PRODUCTION READY ✅

Fitur ini sudah ditest dan siap digunakan!

## Transfer DSJ Camera Videos ✅

**CONFIRMED**: Video DSJ Camera bisa ditransfer via WiFi/USB!

Sistem transfer sekarang scan 2 lokasi:
1. **App Videos**: `/storage/emulated/0/Android/data/com.dahua.nmea/files/Videos/`
2. **DSJ Videos**: `/storage/emulated/0/mpt/` (recursive scan semua subfolder)

Setiap session akan menampilkan:
- Source: "App" atau "DSJ Camera"
- Video file (MP4)
- NMEA file (TXT) jika ada
- Total size

**WiFi Transfer**: Berfungsi normal untuk video DSJ Camera
**USB Transfer**: Berfungsi normal untuk video DSJ Camera
**ZIP Export**: Berfungsi normal untuk video DSJ Camera
