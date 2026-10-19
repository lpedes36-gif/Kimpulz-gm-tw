# KIMPUL TWEAKS 2.0 — Lightweight Edition

Aplikasi Android native ringan untuk pengaturan animasi, backup/restore, informasi perangkat, pemeriksaan mount partisi (hanya baca), dan log tindakan. Metode akses: Root atau Shizuku.

## Batasan penting
- Proyek ini **tidak menulis atau me-remount partisi sistem**. Fitur partisi hanya membaca `/proc/mounts`.
- Shizuku bukan root. Aksesnya bergantung pada versi Android, layanan Shizuku, dan izin yang diberikan.
- Fitur performance/game mengubah tiga skala animasi global Android (1.0x, 0.5x, 0.0x). Tidak ada klaim peningkatan FPS atau perubahan CPU/GPU.
- `settings` dapat ditolak oleh ROM atau kebijakan perangkat. Selalu cek log.
- Backup berada di penyimpanan privat aplikasi; menghapus data aplikasi dapat menghapus backup.
- Gunakan pada perangkat milik sendiri dan tinjau setiap tindakan sebelum menyetujui akses root.

## Build APK melalui GitHub Actions
Ini metode yang direkomendasikan untuk pemula Android karena Android SDK disiapkan di runner GitHub.

1. Unduh dan ekstrak arsip proyek.
2. Buat repositori baru di GitHub.
3. Unggah isi folder proyek ke repositori (file `settings.gradle`, `build.gradle`, folder `app`, dan folder `.github` harus di root repositori).
4. Buka tab **Actions** dan aktifkan Actions bila diminta.
5. Jalankan workflow **Build KIMPUL TWEAKS APK** melalui tombol **Run workflow**, atau push ke branch `main`.
6. Tunggu workflow selesai dengan status hijau.
7. Buka hasil workflow dan unduh artifact `KIMPUL-TWEAKS-v2-debug`.
8. Ekstrak ZIP artifact; file `app-debug.apk` dapat dipindahkan ke HP dan dipasang. Jika Android memblokir pemasangan, izinkan instalasi dari sumber tersebut hanya jika kamu percaya pada APK yang kamu bangun sendiri.

## Build lokal di Termux (opsional)
Build lokal butuh Java 17, Gradle 8.9, Android SDK platform 35, dan build-tools 35.0.0. SDK Android tidak selalu mudah disiapkan di Termux. Jika semua sudah tersedia dan `ANDROID_HOME` mengarah ke SDK:
```bash
pkg update
pkg install openjdk-17 gradle unzip
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
gradle --no-daemon assembleDebug
```
Sesuaikan path `ANDROID_HOME` dengan lokasi SDK yang benar. APK hasil build ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Versi dan konfigurasi
- Android Gradle Plugin: 8.7.3
- Gradle: 8.9
- Java: 17
- compileSdk: 35
- minSdk: 23
- targetSdk: 28 (contoh kompatibilitas; untuk rilis Play Store perlu evaluasi dan peningkatan target SDK sesuai persyaratan terkini)
- Shizuku API: 13.1.5

## Troubleshooting
- **Shizuku belum aktif:** buka aplikasi Shizuku, mulai layanan melalui metode yang didukung perangkat, lalu buka KIMPUL TWEAKS dan beri izin.
- **Root gagal:** pastikan perangkat benar-benar di-root dan berikan permintaan akses di aplikasi pengelola root.
- **Perintah ditolak:** periksa log. Jangan mengatasi kegagalan dengan memaksa remount atau menonaktifkan Verified Boot.
- **Build gagal:** buka detail log Actions; pastikan file proyek berada di root repositori, dan workflow memiliki akses membaca source.


## Perbaikan workflow GitHub Actions

Workflow build APK telah diperbarui untuk menggunakan versi action yang lebih baru dan runner Ubuntu 24.04. Unggah seluruh isi folder proyek ke repositori GitHub, lalu buka tab Actions dan jalankan workflow. APK belum tersedia sampai proses build berhasil.


### Catatan build terbaru
Workflow terbaru menjalankan Gradle 8.9 langsung dan menonaktifkan pembuatan dependency graph, sehingga tidak memerlukan file `gradlew` atau folder `gradle/wrapper`. Pastikan mengganti file `.github/workflows/android.yml` di repositori GitHub dengan versi dari arsip ini, lalu commit perubahan.


## Build GitHub Actions dengan Node.js 24

Workflow `.github/workflows/android.yml` menggunakan `actions/setup-node@v7` dengan Node.js 24, versi Actions terbaru yang mendukung runtime Node 24, Java 17, Android SDK 35, dan Gradle 8.9. Node.js 24 digunakan untuk lingkungan Node pada workflow; aplikasi Android tetap dikompilasi menggunakan Java/Gradle.
