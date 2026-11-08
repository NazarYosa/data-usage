# Data Usage 📊

[![Release](https://img.shields.io/badge/Release-v1.0.0-00E676?style=for-the-badge&logo=android)](https://github.com/NazarYosa/data-usage/releases/tag/v1.0.0)
[![APK Size](https://img.shields.io/badge/APK%20Size-~78%20KB-00E5FF?style=for-the-badge)](https://github.com/NazarYosa/data-usage/releases/tag/v1.0.0)
[![RAM Usage](https://img.shields.io/badge/Idle%20RAM-0%20MB-success?style=for-the-badge)](README.md)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![Compatibility](https://img.shields.io/badge/Android-6.0%20to%2014%2B-blueviolet?style=for-the-badge&logo=android)](README.md)

**Data Usage** adalah aplikasi pemantau penggunaan data internet (Data Seluler & Wi-Fi) ultra-ringan dan hemat daya untuk perangkat Android. Berbeda dari aplikasi sejenis yang membebani memori dan menguras baterai dengan *foreground service* terus-menerus, **Data Usage dirancang secara presisi dengan arsitektur 0 MB RAM saat idle**.

---

## ✨ Fitur Unggulan

- 📱 **Home Screen Widget Modern & Presisi**:
  - Menampilkan kuota **Mobile Data** (atas) dan **Wi-Fi** (bawah) secara terpisah dengan angka besar berwarna dan satuan yang rapi.
  - **Timestamp Pembaruan Terakhir**: Menampilkan waktu tepat pembaruan data dalam format `jam:menit:detik` (`HH:mm:ss`).
  - **Tombol Refresh Manual**: Ketuk tombol refresh langsung di widget untuk memperbarui data seketika (dengan animasi progress loading).
  - **Perhitungan Sisa Kuota (Remaining Quota)**: Menghitung sisa kuota atau kelebihan pemakaian berdasarkan batas kuota yang Anda tentukan.
- ⚡ **0 MB RAM Idle & Sangat Hemat Baterai**:
  - Tidak ada service background atau notifikasi persisten yang berjalan tanpa henti.
  - Menggunakan sistem `AlarmManager` bawaan OS Android yang hanya aktif selama beberapa milidetik untuk me-refresh widget, lalu langsung menutup proses dari memori.
- 🔋 **Dukungan Pengoptimalan Baterai**:
  - Menyediakan tombol akses cepat di dalam aplikasi untuk mematikan pembatasan baterai sistem agar widget selalu ter-update tepat waktu di semua merek HP (Samsung, Xiaomi, Oppo, Vivo, Realme, dll.).
- 🔒 **100% Offline & Menjaga Privasi**:
  - Aplikasi **TIDAK MEMILIKI IZIN INTERNET** (`android.permission.INTERNET`).
  - Bebas dari pelacak, iklan, atau pengiriman data ke server luar. Semua data statistik jaringan dibaca langsung dari kernel Android lokal (`NetworkStatsManager`).
- 💎 **Ukuran File Terkecil di Kelasnya**:
  - Ukuran file APK hanya **~78 KB** (total penggunaan disk + data pengguna di bawah 250 KB).

---

## 📸 Tampilan Antarmuka Widget

```text
┌──────────────────────────────────────────────┐
│  15:46:12                                 🔄 │  <-- Waktu update & tombol refresh
│                                              │
│  📶  Mobile Data                     7.6 GB  │  <-- Data Seluler (Hijau)
│      Sisa 12.4 GB                            │
│  ──────────────────────────────────────────  │  <-- Divider halus
│  📶  Wi-Fi                          28.4 GB  │  <-- Wi-Fi (Biru)
└──────────────────────────────────────────────┘
```

---

## 🛠️ Arsitektur & Performa

| Indikator | Data Usage (Aplikasi Ini) | Aplikasi Monitor Biasa |
| :--- | :--- | :--- |
| **Ukuran APK** | **~78 KB** | 10 MB – 30 MB |
| **Konsumsi RAM (Idle)** | **0 MB** | 40 MB – 120 MB |
| **Background Service** | **Tidak Ada (Zero Service)** | Running Service 24/7 |
| **Izin Internet** | **TIDAK ADA** | Butuh Akses Internet |
| **Kompatibilitas** | Android 6.0 s/d Android 14+ | Tergantung OEM |

---

## 📥 Unduh & Instalasi

File APK resmi siap pasang yang telah ditandatangani:

- 🚀 **[Download Release APK v1.0.0 (Data-Usage-v1.0.0.apk)](https://github.com/NazarYosa/data-usage/releases/download/v1.0.0/Data-Usage-v1.0.0.apk)** — *Ukuran: ~78 KB*
- 📦 **[Halaman Rilis GitHub (v1.0.0)](https://github.com/NazarYosa/data-usage/releases/tag/v1.0.0)**

### Cara Memasang:
1. Unduh file `Data-Usage-v1.0.0.apk` ke HP Android Anda.
2. Buka file APK dan pilih **Install**.
3. Buka aplikasi **Data Usage**:
   - Ketuk **Berikan Akses Penggunaan (Grant Usage Access)** dan aktifkan izinnya.
   - Ketuk **Matikan Pembatasan Baterai (Disable Battery Optimization)** agar widget tidak dibekukan oleh sistem HP saat layar mati.
4. Pergi ke Home Screen (layar utama HP), tekan lama area kosong, pilih menu **Widget**, lalu tambahkan widget **Data Usage**.

---

## 💻 Kompilasi dari Source Code

Pastikan Anda memiliki JDK 17 atau yang lebih baru dan Android SDK (API 34).

```powershell
# Clone repositori
git clone -b v1.0.0 https://github.com/NazarYosa/data-usage.git
cd data-usage

# Kompilasi Release APK
.\gradlew.bat assembleRelease
```

File APK hasil build akan berada di `app/build/outputs/apk/release/app-release.apk`.

---

## 📄 Lisensi

Proyek ini dirilis di bawah lisensi terbuka [MIT License](LICENSE).
