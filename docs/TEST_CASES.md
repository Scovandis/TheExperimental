# Test Case — Reproduksi Flow Robot Gesture Controller

> Test case ini untuk pengujian **manual end-to-end** (device nyata / emulator), melengkapi
> unit test otomatis yang sudah ada di `shared/src/commonTest`. Tujuannya mereproduksi flow
> nyata di [`FLOW.md`](./FLOW.md) dan memverifikasi temuan di
> [`AUDIT_INCOMPLETE.md`](./AUDIT_INCOMPLETE.md) dengan hasil pengujian aktual — bukan asumsi.
>
> Kolom **Status/Hasil Aktual** sengaja dikosongkan untuk diisi tester. Kolom **Ekspektasi**
> mencerminkan perilaku kode SAAT INI (termasuk bug yang sudah diketahui, ditandai ⚠️) — bukan
> perilaku ideal, supaya test ini bisa dipakai untuk verifikasi regresi apa adanya.

## Persiapan Lingkungan

| Platform | Cara jalan | Catatan |
|---|---|---|
| Android | Install `androidApp` ke device/emulator fisik dengan kamera depan | Satu-satunya platform dengan hand tracking nyata |
| Desktop | `./gradlew :desktopApp:run` | Tanpa kamera — hanya jalur kontrol manual |
| iOS | Build `iosApp` lewat Xcode ke simulator/device | Tanpa kamera — hanya jalur kontrol manual |

Untuk TC yang melibatkan gestur kamera, gunakan **Android** dengan pencahayaan cukup dan tangan
kanan menghadap kamera depan (kamera dicerminkan otomatis).

**Definisi gestur** (`HandGestureClassifier.kt` + `GesturePattern.kt`), berbasis identitas/jumlah
jari yang terbuka — seperti berhitung dengan tangan, bukan posisi/kemiringan tangan di frame —
dipakai sebagai langkah uji:

| Aksi | Pola tangan |
|---|---|
| MOVE_FORWARD | 1 jari terbuka: telunjuk saja |
| MOVE_BACKWARD | 2 jari terbuka: telunjuk + tengah |
| ROTATE_LEFT | 3 jari terbuka: telunjuk + tengah + manis |
| ROTATE_RIGHT | 4 jari terbuka: telunjuk + tengah + manis + kelingking, **tanpa** jempol |
| CROUCH | 5 jari terbuka: telapak penuh **termasuk** jempol |
| IDLE | Kepalan tangan (0 jari), atau kombinasi jari lain di luar pola di atas |
| EMERGENCY STOP (gestur) | Kepalan tangan **+ ibu jari terentang** (4 jari lain tetap tertutup), tahan ±1 detik |

---

## A. Alur Gestur Normal

### TC-A01 — Deteksi gestur MOVE_FORWARD dan pergerakan robot maju

- **Precondition:** App Android terbuka, izin kamera diberikan, `RobotStage` menampilkan robot diam (IDLE).
- **Langkah:**
  1. Tunjukkan 1 jari (telunjuk saja, jari lain + jempol terlipat) ke kamera, tahan ±0.5 detik.
  2. Amati `GestureAndHandInfoCard` (confidence, aksi stabil) dan `RobotStage`.
- **Ekspektasi:**
  - Setelah confidence ≥ 0.85 bertahan 300ms, aksi stabil berubah jadi `MOVE_FORWARD`.
  - Robot pada `RobotStage` mulai animasi jalan maju (posisi Z bertambah, `walkPhase` bergerak).
  - `CenterActionDeck` menampilkan aksi saat ini "MOVE_FORWARD".
- **Status/Hasil Aktual:** _(isi tester)_

### TC-A02 — Deteksi gestur CROUCH

- **Langkah:** Tunjukkan 5 jari (telapak terbuka penuh, termasuk jempol), tahan ±0.5 detik.
- **Ekspektasi:** Aksi stabil → `CROUCH`; `RobotState.scaleY` mengecil (robot terlihat merunduk).
- **Status/Hasil Aktual:** _(isi tester)_

### TC-A03 — Deteksi gestur MOVE_BACKWARD

- **Langkah:** Tunjukkan 2 jari (telunjuk + tengah, jari lain + jempol terlipat), tahan ±0.5 detik.
- **Ekspektasi:** Aksi stabil → `MOVE_BACKWARD`; posisi Z robot berkurang.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-A04 — Deteksi gestur ROTATE_LEFT / ROTATE_RIGHT

- **Langkah:** Tunjukkan 3 jari (telunjuk+tengah+manis, tanpa jempol) untuk `ROTATE_LEFT`, lalu 4 jari (+ kelingking, tetap tanpa jempol) untuk `ROTATE_RIGHT`, masing-masing tahan ±0.5 detik.
- **Ekspektasi:** Aksi stabil berubah `ROTATE_LEFT` saat 3 jari, `ROTATE_RIGHT` saat 4 jari; `rotationY` robot berubah arah sesuai.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-A05 — Kepalan tangan biasa → IDLE (bukan darurat)

- **Langkah:** Kepalkan tangan **tanpa** merentangkan ibu jari, tahan 2 detik.
- **Ekspektasi:** Aksi tetap `IDLE`, **tidak** memicu Emergency Stop (karena syarat darurat butuh ibu jari terentang juga — lihat TC-B01). `emergencyProgress` di HUD tetap 0.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-A06 — Stabilisasi temporal menahan gestur sekilas (hysteresis)

- **Langkah:** Tunjukkan gestur MOVE_FORWARD hanya sekilas (<300ms) lalu kembali ke posisi netral/IDLE.
- **Ekspektasi:** Aksi stabil **tidak** langsung berubah ke `MOVE_FORWARD` — `TemporalStabilizer` butuh confidence ≥0.85 bertahan 300ms berturut-turut sebelum lock. Aksi stabil tetap `IDLE` atau aksi sebelumnya.
- **Status/Hasil Aktual:** _(isi tester)_

---

## B. Alur Keselamatan (Safety / Emergency)

### TC-B01 — Emergency Stop via gestur (kepalan + ibu jari)

- **Precondition:** Robot dalam kondisi bergerak (mis. hasil TC-A01).
- **Langkah:**
  1. Kepalkan tangan dengan ibu jari terentang, tahan penuh ±1 detik (amati `emergencyProgress` naik ke 100%).
- **Ekspektasi:**
  - Setelah genap 1000ms, `HaltMode` → `EMERGENCY_STOP` (latched).
  - Banner merah "EMERGENCY_STOP AKTIF" muncul di atas layar dengan tombol "RESET ROBOT".
  - Robot berhenti total seketika (speed/rotation dipaksa 0), animasi jalan berhenti.
  - Melepas kepalan/menjauh dari kamera **tidak** membuat robot jalan lagi — latch hanya lepas lewat Reset (lihat TC-B04).
- **Status/Hasil Aktual:** _(isi tester)_

### TC-B02 — Emergency Stop via tombol UI

- **Precondition:** Robot bergerak (gestur apa pun).
- **Langkah:** Tekan tombol "EMERGENCY STOP" di `CenterActionDeck`.
- **Ekspektasi:** Sama seperti TC-B01 — halt seketika, banner muncul, latch bertahan meski gestur kamera berubah-ubah setelahnya.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-B03 — Tangan hilang dari frame → STOP → SAFETY_LOCK

- **Precondition:** Robot bergerak dengan gestur aktif.
- **Langkah:**
  1. Sembunyikan tangan dari kamera, ukur waktu dengan stopwatch.
  2. Amati `HaltMode` pada ~300ms dan pada ~2000ms.
- **Ekspektasi:**
  - Pada ≥300ms tanpa tangan terdeteksi: `HaltMode` → `STOP` (`StopReason` terkait hand-lost), robot berhenti, tapi belum ter-latch.
  - Pada ≥2000ms tanpa tangan terdeteksi: `HaltMode` naik ke `SAFETY_LOCK` (latched) — banner merah muncul, butuh Reset untuk pulih meskipun tangan kembali ke frame.
  - Jika tangan **kembali ke frame sebelum 2000ms**: `HaltMode` pulih otomatis ke `RUNNING` tanpa perlu Reset.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-B04 — Reset Robot melepas latch

- **Precondition:** `HaltMode` sedang `EMERGENCY_STOP` atau `SAFETY_LOCK` (dari TC-B01/B02/B03).
- **Langkah:** Tekan "RESET ROBOT" pada banner atau tombol Reset di `CenterActionDeck`.
- **Ekspektasi:** Banner hilang, `HaltMode` → `RUNNING`, posisi/rotasi robot kembali ke state awal (`RobotState()` default), confidence & lock progress ter-reset ke 0, robot siap menerima gestur baru.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-B05 — Confidence rendah/ambigu → STOP sementara

- **Langkah:** Tunjukkan pose tangan yang ambigu/transisi cepat antar gestur (mis. jari setengah tertekuk) sehingga confidence turun di bawah 0.50.
- **Ekspektasi:** `HaltMode` → `STOP` dengan alasan `LOW_CONFIDENCE`; begitu pose kembali jelas dan confidence naik, `HaltMode` pulih otomatis ke `RUNNING` tanpa perlu Reset (karena `STOP` bukan latch).
- **Status/Hasil Aktual:** _(isi tester)_

---

## C. Alur Kalibrasi (termasuk verifikasi bug #2 di audit)

### TC-C01 — Menyelesaikan wizard kalibrasi 6 langkah

- **Langkah:**
  1. Buka tab "Calibration" di bottom dock.
  2. Ikuti wizard: CENTER → UP → DOWN → LEFT → RIGHT → FIST, tahan tangan sesuai instruksi tiap langkah sampai progress penuh (≥12 sampel), tekan "Next" tiap langkah.
  3. Selesaikan langkah terakhir (FIST).
- **Ekspektasi:** Modal wizard tertutup otomatis setelah langkah terakhir; tidak ada error.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-C02 — Verifikasi hasil kalibrasi diterapkan ke pipeline aktif ⚠️ (reproduksi bug audit #2)

- **Precondition:** Selesaikan TC-C01 dengan pola tangan yang **sengaja sedikit berbeda** dari
  default (mis. tangan lebih kecil dari layar/sudut kamera berbeda) sehingga hasil kalibrasi
  seharusnya menggeser ambang batas gestur secara terukur.
- **Langkah:**
  1. Sebelum kalibrasi: uji satu gestur borderline (misalnya crouch dengan wrist Y persis di
     sekitar ambang batas default) dan catat hasil klasifikasinya.
  2. Selesaikan wizard kalibrasi (TC-C01) dengan data tangan yang berbeda dari default.
  3. Ulangi gestur borderline yang sama persis seperti langkah 1, tanpa restart app.
- **Ekspektasi (perilaku kode SAAT INI — bug):** Hasil klasifikasi gestur borderline **tidak
  berubah** meski kalibrasi baru saja diselesaikan — karena `calibrationProfile` hasil wizard
  tersimpan di variabel lokal `RobotControlViewModel` dan tidak pernah diterapkan ulang ke
  `GestureConfig`/`ControlSpaceConfig` yang sudah dikonstruksi (lihat
  [`AUDIT_INCOMPLETE.md` #2](./AUDIT_INCOMPLETE.md#2-hasil-kalibrasi-tidak-pernah-diterapkan-ke-pipeline-aktif)).
- **Ekspektasi ideal (setelah bug diperbaiki):** Klasifikasi gestur borderline berubah mengikuti
  profil kalibrasi baru.
- **Status/Hasil Aktual:** _(isi tester — tandai apakah bug masih ada di build yang diuji)_

### TC-C03 — Cancel kalibrasi di tengah wizard

- **Langkah:** Mulai wizard kalibrasi, batalkan (tombol Cancel) sebelum langkah terakhir.
- **Ekspektasi:** Modal tertutup, tidak ada perubahan pada profil kalibrasi aktif, rekaman sampel dibuang.
- **Status/Hasil Aktual:** _(isi tester)_

---

## D. Alur Kontrol Manual (termasuk verifikasi bug #4 di audit)

### TC-D01 — Tap kartu gestur manual mengunci aksi secara permanen ⚠️ (reproduksi bug audit #4, paling kritis)

- **Precondition:** Kamera aktif dan mendeteksi tangan dengan baik (gestur kamera normal berfungsi).
- **Langkah:**
  1. Di kolom "Peta Gestur", tap sekali kartu "Maju"/"MOVE_FORWARD".
  2. Lepas jari dari layar (tap sudah selesai).
  3. Sekarang tunjukkan gestur **CROUCH** yang jelas ke kamera dan tahan.
- **Ekspektasi (perilaku kode SAAT INI — bug):** Robot **tetap** menjalankan `MOVE_FORWARD`
  meski gestur kamera menunjukkan `CROUCH` dengan jelas — override manual mengunci dan
  mengabaikan input kamera sepenuhnya, karena `onManualActionReleased()` tidak pernah dipanggil
  dari UI (lihat
  [`AUDIT_INCOMPLETE.md` #4](./AUDIT_INCOMPLETE.md#4-kontrol-manual-tap-gestur-macet-permanen--stuck-override-bug-paling-serius)).
- **Langkah lanjutan:** Tekan "RESET ROBOT" atau "EMERGENCY STOP".
- **Ekspektasi:** Setelah Reset/Emergency Stop, override manual lepas (`manualAction = null`) dan gestur kamera kembali berfungsi normal.
- **Status/Hasil Aktual:** _(isi tester — catat apakah override lepas dengan cara lain selain Reset/Emergency Stop)_

### TC-D02 — Kontrol manual di platform tanpa kamera (Desktop/iOS)

- **Precondition:** Jalankan `desktopApp` atau `iosApp`.
- **Langkah:**
  1. Amati status tracker di UI (harus menunjukkan pesan "Kontrol gestur hanya tersedia di Android. Gunakan kontrol manual.").
  2. Tap kartu gestur manual mana pun.
- **Ekspektasi:** Robot mengikuti aksi yang di-tap dan **tetap terkunci** pada aksi itu (bug yang sama seperti TC-D01) — karena ini satu-satunya jalur kontrol di platform ini, dampaknya lebih terasa di sini.
- **Status/Hasil Aktual:** _(isi tester)_

---

## E. Alur Tampilan & Navigasi

### TC-E01 — Toggle mode render 3D ↔ 2D

- **Langkah:** Tekan toggle "Mode & View" (3D/2D) di kolom kanan.
- **Ekspektasi:** `RobotStage` berganti antara render 3D (Filament, model robot solid dengan pencahayaan) dan render 2D (siluet Compose Canvas datar), tanpa reset posisi/state robot.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-E02 — Zoom panggung 3D tidak dapat diakses ⚠️ (verifikasi audit #5)

- **Langkah:** Pada mode render 3D, coba cari kontrol zoom (+/-) di sekitar `RobotStage`.
- **Ekspektasi (perilaku kode SAAT INI):** **Tidak ada kontrol zoom yang terlihat di UI** —
  meskipun `RobotControlViewModel.zoomIn()`/`zoomOut()` sudah berfungsi penuh secara logika,
  tidak ada tombol yang memanggilnya (lihat
  [`AUDIT_INCOMPLETE.md` #5](./AUDIT_INCOMPLETE.md#5-komponen-ui-dibangun-tapi-tidak-pernah-dipakai-dead-code-ui)).
- **Status/Hasil Aktual:** _(isi tester)_

### TC-E03 — Toggle "Simulation / Real Robot" tidak berefek ⚠️ (verifikasi audit #3)

- **Langkah:** Tekan pill "Simulation"/"Real Robot" di kartu "Mode & View" berulang kali sambil menjalankan gestur normal.
- **Ekspektasi (perilaku kode SAAT INI):** Tidak ada perubahan perilaku apa pun pada safety gate atau pergerakan robot — toggle ini murni visual lokal (lihat
  [`AUDIT_INCOMPLETE.md` #3](./AUDIT_INCOMPLETE.md#3-mode-simulation-vs-real-robot-hanya-kosmetik)).
- **Status/Hasil Aktual:** _(isi tester)_

### TC-E04 — Tombol Settings/Menu di header adalah no-op

- **Langkah:** Tekan ikon gear (Settings) dan ikon hamburger (Menu) di header.
- **Ekspektasi:** Tidak ada dialog/drawer/navigasi yang muncul — tidak terjadi apa pun.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-E05 — Tombol "Edit" (Peta Gestur) dan "Detail" (Telemetry) adalah no-op

- **Langkah:** Tekan tombol "Edit" di kartu Peta Gestur, lalu tombol "Detail" di Telemetry Card.
- **Ekspektasi:** Tidak ada aksi/navigasi yang terjadi pada keduanya.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-E06 — Tab Record / Replay / Settings di bottom dock tanpa fitur

- **Langkah:** Tekan tab "Record", lalu "Replay", lalu "Settings" di bottom navigation dock.
- **Ekspektasi:** Hanya label tab aktif yang berubah (indikator visual), tidak ada layar/fitur baru yang muncul untuk ketiganya.
- **Status/Hasil Aktual:** _(isi tester)_

### TC-E07 — Layout responsif (lebar vs sempit)

- **Langkah:** Jalankan di device/window dengan lebar ≥860dp (tablet/landscape), lalu di lebar sempit (ponsel portrait).
- **Ekspektasi:** Layout ≥860dp menampilkan 3 kolom sejajar (kamera+telemetry | panggung robot | peta gestur). Layout sempit menampilkan susunan vertikal yang bisa di-scroll, urutan: panggung robot → action deck → kamera → peta gestur → info → telemetry → mode & view → tips.
- **Status/Hasil Aktual:** _(isi tester)_

---

## F. Cakupan Test Otomatis (referensi, bukan langkah manual)

Unit test domain sudah berjalan otomatis via Gradle:

```
./gradlew :shared:allTests
```

11 file di `shared/src/commonTest/.../robot/` men-cover logika murni (klasifikasi gestur,
interpreter, safety, motion engine, dll — lihat [`FLOW.md` §11](./FLOW.md#11-testing-otomatis-yang-sudah-ada)).
Ini **tidak** mencakup:
- `RobotControlViewModel` (orkestrator — termasuk bug D01/C02 di atas)
- Integrasi kamera/MediaPipe nyata (butuh device fisik, tidak bisa unit test)
- Rendering Filament (visual, butuh verifikasi manual — TC-E01)

Untuk area ini, test case manual A–E di atas adalah satu-satunya bentuk verifikasi yang ada
saat ini di project.

---

## Ringkasan Cara Mengisi

Untuk setiap TC: jalankan langkah persis seperti tertulis, isi kolom **Status/Hasil Aktual**
dengan `PASS` (sesuai ekspektasi) atau `FAIL` (beda dari ekspektasi) + deskripsi singkat apa
yang benar-benar terjadi, plus device/platform yang dipakai. TC bertanda ⚠️ adalah reproduksi
bug yang **sudah diketahui** di audit — hasil "sesuai ekspektasi (bug masih ada)" berarti bug
belum diperbaiki; kalau perilakunya justru sudah benar, berarti bug tersebut sudah pernah
diperbaiki dan `AUDIT_INCOMPLETE.md` perlu diperbarui.
