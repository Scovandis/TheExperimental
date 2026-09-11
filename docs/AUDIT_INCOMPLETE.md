# Audit: Fitur Belum Selesai & Flow yang Tidak Sinkron

> Semua temuan di bawah diverifikasi langsung dari source code (bukan asumsi/tebakan) per
> commit `c42e003`. Referensi file:line akurat pada state ini — cek ulang jika file sudah
> berubah. Lihat [`FLOW.md`](./FLOW.md) untuk konteks arsitektur, dan
> [`TEST_CASES.md`](./TEST_CASES.md) untuk cara mereproduksi tiap temuan secara manual.
>
> Catatan: **tidak ditemukan satu pun** komentar `TODO`/`FIXME`/`HACK`/"belum diimplementasi" di
> seluruh repo. Semua temuan berikut adalah gap yang **tidak ditandai** di kode — murni hasil
> penelusuran logika end-to-end.

## Ringkasan Prioritas

> Nomor pada tabel ini **sama dengan** nomor heading `##` di bagian detail masing-masing di
> bawah — bukan urutan severity. Urutkan sendiri secara visual lewat kolom Severity bila perlu.

| # | Temuan | Severity | Kategori |
|---|---|---|---|
| 1 | Tiga implementasi renderer robot 3D, satu jadi dead code | 🟠 Tinggi | Dead code / kebingungan arsitektur |
| 2 | Hasil kalibrasi tidak pernah diterapkan ke pipeline aktif | 🔴 Kritis | Bug fungsional |
| 3 | Toggle "Simulation / Real Robot" murni kosmetik | 🟠 Tinggi | Flow tidak sinkron |
| 4 | Kontrol manual (tap gestur) macet permanen — stuck override | 🔴 Kritis | Bug fungsional |
| 5 | Komponen UI duplikat (HUD, safety banner, telemetry, zoom) yang tak terpakai | 🟠 Tinggi | Dead code |
| 6 | Tombol Settings, Menu, Edit, Detail — no-op | 🟡 Sedang | Action belum ada |
| 7 | Tab Record / Replay / Settings di bottom dock — tanpa fitur | 🟡 Sedang | Fitur belum ada |
| 8 | Kamera & hand tracking hanya Android; iOS/Desktop stub | 🟢 Rendah (by design) | Keterbatasan platform |
| 9 | Cakupan test kosong untuk kelas kritis (termasuk ViewModel) | 🟠 Tinggi | Test coverage |

Catatan tambahan yang dibahas di dalam temuan #5: `ManualControlPad` (pad hold-to-move yang
sudah dibangun benar) dan `ZoomControls` (logika zoom sudah lengkap) sama-sama tidak pernah
disambungkan ke layar — keduanya solusi siap pakai untuk masalah di temuan #4 dan zoom yang
tak bisa diakses.

---

## 1. Tiga implementasi renderer robot 3D — hanya 2 yang terpakai

**Lokasi:** `shared/src/commonMain/kotlin/com/experimental/robot/presentation/render/*.kt`
(`Mat4.kt`, `Mesh.kt`, `ObjMeshLoader.kt`, `RobotMeshBuilder.kt`, `SoftwareRenderer.kt`, `Vec3.kt`)

Repo punya **tiga** model robot yang berbeda, dibangun terpisah:

1. `Robot3dCanvas.kt` — render 3D produksi via **Filament KMP** (dipakai, mode `THREE_D`).
2. `RobotCanvas.kt` — render 2D prosedural via Compose `Canvas` (dipakai, mode `TWO_D`).
3. `presentation/render/*` — rasterizer software lengkap (perspective projection, back-face
   culling, Lambert shading) + parser OBJ/MTL sendiri. **Tidak ada satu pun pemanggilan**
   `RobotMeshBuilder`, `SoftwareRenderer(`, atau `ObjMeshLoader.` di luar file test
   (`Render3dTest.kt`, `ObjMeshLoaderTest.kt`) — dikonfirmasi lewat pencarian di seluruh
   `shared/src`.

**Kemungkinan penyebab:** ini kemungkinan besar renderer awal sebelum Filament diintegrasikan
(sesuai commit log `"redering robot"` → `"implement robot 3d menggunakan library filament kmp"`),
dan tidak pernah dihapus setelah migrasi ke Filament.

**Dampak:** kebingungan bagi developer baru (mana yang "benar"), maintenance ganda tanpa manfaat,
build time & ukuran binary lebih besar dari perlu.

**Rekomendasi:** hapus `presentation/render/*` beserta test-nya jika Filament sudah final, atau
—jika software renderer memang disiapkan sebagai fallback untuk device tanpa GPU—dokumentasikan
niat itu dan sambungkan sebagai opsi render mode ketiga yang benar-benar bisa dipilih user.

---

## 2. Hasil kalibrasi tidak pernah diterapkan ke pipeline aktif

**Lokasi:** `RobotControlViewModel.kt:69-70` (`var calibrationProfile`),
`RobotControlViewModel.kt:339-343` (`finishCalibration()`), `RobotGraph.kt:40-59`
(`var calibration`, `createRobotControlViewModel()`)

**Alur yang seharusnya terjadi:** wizard kalibrasi merekam sampel tangan pengguna → menghitung
`CalibrationProfile` baru → profil itu di-`applyTo()`-kan ke `ControlSpaceConfig` → posisi netral
control-space jadi lebih akurat untuk tangan pengguna tersebut.
>
> Catatan (setelah klasifikasi gestur dipindah ke identitas jari / `GesturePattern`):
> `CalibrationProfile` tidak lagi punya `applyTo(GestureConfig)` — tidak ada lagi ambang
> posisi/kemiringan tangan pada klasifikasi gestur yang perlu dikalibrasi. Bug di bawah ini kini
> murni soal `ControlSpaceConfig` (posisi netral), bukan lagi soal `GestureConfig`.

**Yang benar-benar terjadi:**
- `RobotGraph.calibration` di-`applyTo()`-kan **hanya sekali**, saat `createRobotControlViewModel()`
  dipanggil (`RobotGraph.kt:43-44`) — yaitu saat ViewModel pertama kali dibuat.
- `finishCalibration()` (dipanggil setelah wizard 6 langkah selesai) menyimpan hasil kalibrasi ke
  **`calibrationProfile` milik `RobotControlViewModel` sendiri** (variabel lokal, line 69), **bukan**
  ke `RobotGraph.calibration`.
- Bahkan jika ditulis ke `RobotGraph.calibration`, itu tidak akan berefek — `classifier`,
  `interpreter`, dll. sudah dikonstruksi dengan config yang immutable saat ViewModel dibuat
  (`RobotGraph.kt:48,52`); tidak ada mekanisme rebuild config setelah kalibrasi selesai.

**Dampak:** pengguna menyelesaikan wizard kalibrasi 6 langkah (proses ini memakan waktu &
usaha), UI menunjukkan progress dan "selesai", tapi **posisi netral control-space yang
benar-benar dipakai tidak berubah sama sekali**. Fitur ini secara fungsional adalah no-op yang
terlihat seperti bekerja.

**Rekomendasi:** setelah `finishCalibration()`, tulis hasil ke `RobotGraph.calibration` lalu
buat ulang (atau re-konfigurasi) `interpreter` yang dipakai ViewModel — atau ubah
`ControlSpaceConfig` menjadi `mutableStateOf`/reaktif yang dibaca ulang tiap tick.

---

## 3. Mode "Simulation vs Real Robot" hanya kosmetik

**Lokasi:** `RobotControlScreen.kt:153-157` (`ModeAndViewCard` dipanggil tanpa
`onSelectSimulation`/`onSelectRealRobot`), `DashboardComponents.kt:1061-1062,1088`
(default `{}`, `remember { mutableStateOf(true) }` lokal), `RobotControlViewModel.kt:228`
(`requireRobotLink = false` hardcoded)

Pill toggle "Simulation" / "Real Robot" di kartu "Mode & View" hanya mengubah state lokal
Compose (`remember`) — tidak ada callback yang diteruskan ke ViewModel. Konsisten dengan itu,
`SafetySignals.requireRobotLink` selalu `false` di setiap tick, apa pun pilihan pengguna di UI.

**Dampak:** empat dari delapan gerbang `SafetyController` (link robot, baterai, command
timeout, obstacle — lihat [`FLOW.md` §5](./FLOW.md#5-flow-keselamatan-safety--emergency-stop))
**tidak pernah aktif** di build ini karena selalu di-short-circuit ke `RUNNING`. Tidak ada
integrasi robot fisik nyata di manapun di codebase — semua field terkait (`linkConnected`,
`batteryPercent`, `obstacleDistanceCm`, `lastAckAtMs`) di `SafetySignals` tidak pernah diisi
dari sumber nyata.

**Rekomendasi:** jika integrasi robot fisik memang direncanakan, ini flow yang paling penting
untuk diselesaikan lebih dulu (safety-critical). Jika saat ini project memang scope-nya hanya
simulasi, sebaiknya toggle ini disembunyikan/dinonaktifkan agar tidak menyesatkan pengguna.

---

## 4. Kontrol manual (tap gestur) macet permanen — stuck override bug paling serius

**Lokasi:** `RobotControlScreen.kt:142-148,193-196` (`PetaGesturGrid` → `onManualActionPressed`
saja), `RobotControlViewModel.kt:251-259` (`onManualActionPressed` vs `onManualActionReleased`),
`DashboardComponents.kt:800-852` (`.clickable { onGestureClick(...) }`, tap sekali)

Enam kartu gestur di "Peta Gestur" memanggil `viewModel.onManualActionPressed(action)` saat
di-tap. Fungsi pasangannya, `onManualActionReleased()`, **ada di ViewModel tapi dikonfirmasi
tidak pernah dipanggil dari Composable manapun** di seluruh `shared/src` (dicek dengan grep,
zero hasil di luar definisinya sendiri).

Karena `tick()` (`RobotControlViewModel.kt:196-207`) selalu memprioritaskan `manualAction` di
atas `snapshot` kamera selama `manualAction != null`:

**Satu kali tap kartu gestur mana pun akan mengunci robot ke aksi itu secara permanen** —
melewati/mengabaikan gestur tangan asli dari kamera — sampai pengguna menekan **Emergency Stop**
atau **Reset Robot** (keduanya menyetel `manualAction = null`).

**Dampak:** ini adalah bug UX yang cukup serius untuk fitur inti aplikasi (kontrol gestur) —
pengguna yang bermaksud "coba sekali" lewat panel manual justru kehilangan kendali gestur kamera
tanpa indikasi jelas mengapa, sampai mereka menyadari harus menekan Reset.

**Rekomendasi termudah:** ganti `.clickable { }` di `PetaGesturGrid` dengan pola *press-and-hold*
(`pointerInput` + `detectTapGestures` dengan `onPress`/`tryAwaitRelease`, atau langsung pakai
`ManualControlPad` yang sudah dibangun benar — lihat temuan #9) sehingga
`onManualActionReleased()` terpanggil saat jari diangkat dari layar.

---

## 5. Komponen UI dibangun tapi tidak pernah dipakai (dead code UI)

Ditemukan dua "generasi" UI berbeda untuk fitur yang sama — satu set komponen reusable yang
lengkap, dan satu set implementasi inline berbeda yang benar-benar ditampilkan. Tidak ada
penanda TODO yang menjelaskan mana yang final.

| File | Komponen tak terpakai | Yang benar-benar dipakai sebagai gantinya |
|---|---|---|
| `GestureHud.kt` | `ActionBadge`, `TelemetryPanel`, `GestureLegend`, `RenderModeToggle`, `ZoomControls`, `StatusBanner` | Implementasi inline di `DashboardComponents.kt`/`RobotControlScreen.kt` |
| `SafetyControls.kt` | `EmergencyStopButton`, `SafetyBanner`, `CalibrationLauncher` | Banner merah inline di `RobotControlScreen.kt:232-278`; tombol di `CenterActionDeck` |
| `ManualControlPad.kt` | Seluruh file — pad hold-to-move dengan `onPress`/`tryAwaitRelease` yang benar | `PetaGesturGrid` (yang justru punya bug #4) |

**Catatan khusus `ZoomControls`:** `RobotControlViewModel.zoomIn()`/`zoomOut()` (line 273-284)
sudah lengkap dan benar, `ZoomControls` composable-nya juga sudah dibuat
(`GestureHud.kt:344-374`) — tapi **tidak pernah dirender** di `RobotControlScreen.kt`. Fitur
zoom panggung 3D sepenuhnya tidak bisa diakses pengguna meski logikanya sudah selesai
sepenuhnya. Ini murni "lupa disambung ke layar", bukan fitur belum dikerjakan.

**Rekomendasi:** putuskan mana implementasi final, hapus yang lain. Untuk zoom khususnya —
tinggal tambahkan `ZoomControls` (atau tombol +/-) ke `RobotStage`/`ModeAndViewCard`, effort
kecil dengan payoff langsung karena logikanya sudah 100% siap.

---

## 6. Tombol/aksi UI yang no-op (empty lambda)

| Kontrol | Lokasi | Status |
|---|---|---|
| Ikon Settings (⚙) di header | `RobotControlScreen.kt:75` | `{ /* Settings dialog/action */ }` — literal kosong |
| Ikon Menu (☰) di header | `RobotControlScreen.kt:76` | `{ /* Drawer/Menu action */ }` — literal kosong |
| Tombol "Edit" di Peta Gestur | `DashboardComponents.kt:772` | `.clickable { }` — literal kosong |
| Tombol "Detail" di Telemetry Card | `DashboardComponents.kt:353` (default param, tak pernah di-override) | tidak pernah terhubung ke aksi apa pun |

**Dampak:** rendah secara fungsional (tidak safety-critical), tapi user-facing — tombol yang
terlihat interaktif tapi tidak berbuat apa-apa menurunkan kepercayaan pengguna terhadap
aplikasi.

---

## 7. Tab bottom dock tanpa fitur di belakangnya

**Lokasi:** `RobotControlScreen.kt:220-228` (`onTabSelect`), `DashboardComponents.kt:1198-1268`
(`BottomNavigationDock`)

Dari 5 tab (`Home`, `Calibration`, `Record`, `Replay`, `Settings`), **hanya `Calibration`** yang
memicu aksi nyata (`viewModel.startCalibration()`). Tab lain hanya mengubah label `currentTab`
lokal tanpa efek lain. `Record`/`Replay` khususnya menyiratkan fitur merekam & memutar ulang
sesi gestur/gerakan robot — **tidak ada backing logic untuk ini di mana pun** di codebase (tidak
ada model data recording, tidak ada file persistence terkait).

**Rekomendasi:** jika `Record`/`Replay`/`Settings` memang di roadmap, ini kandidat kuat untuk
task terpisah. Jika tidak, sembunyikan tab-tab ini agar tidak menjanjikan fitur yang belum ada.

---

## 8. Kamera & hand tracking hanya Android (keterbatasan by design, bukan bug)

**Lokasi:** `CameraFeed.ios.kt:23-45`, `CameraFeed.jvm.kt:23-45`

Implementasi `actual CameraFeed` untuk iOS dan Desktop langsung mempublikasikan
`TrackerStatus.Unsupported("Kontrol gestur hanya tersedia di Android. Gunakan kontrol manual.")`
dan menampilkan placeholder statis — tidak ada binding AVFoundation (iOS) atau webcam (Desktop)
sama sekali. Ini konsisten dengan `build.gradle.kts` yang hanya mendeklarasikan CameraX +
MediaPipe di `androidMain`.

**Ini bukan bug** — tapi penting dicatat karena pesan di stub eksplisit mengarahkan pengguna ke
"kontrol manual" sebagai fallback, padahal jalur kontrol manual yang benar-benar tersambung
punya bug stuck-override (temuan #4). **Di iOS/Desktop, satu-satunya cara menggerakkan robot
adalah tap sekali per gestur yang lalu terkunci** sampai Reset/Emergency Stop ditekan — jauh
dari pengalaman "kontrol manual" yang layak.

**Rekomendasi:** prioritaskan perbaikan #4 sebelum menganggap iOS/Desktop punya fallback yang
memadai. Implementasi kamera native iOS/Desktop sendiri adalah pekerjaan besar terpisah, di luar
cakupan audit ini.

---

## 9. Cakupan test yang kosong

**Lokasi:** `shared/src/commonTest/`, `androidHostTest/`, `iosTest/`, `jvmTest/`

11 file test domain sudah ada dan solid (~124 `@Test`) — lihat [`FLOW.md` §11](./FLOW.md#11-testing-otomatis-yang-sudah-ada).
Namun kelas berikut **tidak punya test sama sekali**:

- `RobotControlViewModel` — **orkestrator seluruh pipeline, termasuk bug #2 dan #4 di atas** —
  seandainya ada test integrasi untuk alur manual-press/release atau kalibrasi→apply, kedua bug
  ini kemungkinan besar akan tertangkap sebelum sampai ke UI.
- `RobotGraph` (wiring DI)
- `HandLandmarkStream` (buffering, reset saat hand invalid)
- `FrameRateMeter`
- `EmergencyGestureDetector` (safety-critical — deteksi darurat tidak punya unit test langsung,
  hanya tervalidasi lewat penelusuran manual di audit ini)
- `LandmarkSmoother`
- `FingerExtensionDetector`, `SlewRateLimiter` (hanya teruji tidak langsung lewat test kelas lain)

Sebaliknya, `ObjMeshLoaderTest.kt` dan `Render3dTest.kt` (22 test gabungan) menguji renderer
software yang **sudah jadi dead code** (temuan #1) — usaha maintenance test untuk kode yang
tidak pernah jalan di produksi.

File `SharedCommonTest.kt` + test bawaan template di `androidHostTest`/`iosTest`/`jvmTest`
(4 file) masih berisi boilerplate wizard KMP (`assertEquals(3, 1 + 2)`) — tidak menguji apa pun
yang nyata.

**Rekomendasi:** prioritas tertinggi adalah test integrasi untuk `RobotControlViewModel`
(khususnya alur manual action & kalibrasi) dan unit test untuk `EmergencyGestureDetector` karena
sifatnya safety-critical.

---

## Ringkasan Rekomendasi Perbaikan (urutan prioritas)

1. Perbaiki stuck-override kontrol manual (#4) — fungsional inti, effort kecil-menengah.
2. Sambungkan hasil kalibrasi ke pipeline aktif (#2) — fungsional inti, effort menengah.
3. Putuskan status integrasi robot fisik; kalau belum, sembunyikan toggle Real Robot (#3).
4. Sambungkan `ZoomControls` yang logikanya sudah selesai (#5) — effort kecil, payoff langsung.
5. Bersihkan dead code renderer & komponen UI duplikat (#1, #5) — mengurangi kebingungan
   maintenance.
6. Tambah test untuk `RobotControlViewModel` dan `EmergencyGestureDetector` (#9).
7. Putuskan nasib tombol/tab no-op (#6, #7) — implementasikan atau sembunyikan.
