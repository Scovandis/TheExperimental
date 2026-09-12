# Gesture Robot Controller (KMP + MVVM)

Aplikasi Kotlin Multiplatform (Android / Desktop / iOS) yang mengendalikan animasi robot
prosedural memakai gestur tangan. Pipeline Android: **CameraX → MediaPipe Hand Landmarker →
Gesture Classifier → Temporal Stabilizer → Motion Engine → Filament / Compose Canvas**.

## Peta gestur

Klasifikasi murni berdasarkan **identitas jari yang terbuka** — sama seperti berhitung
dengan tangan, bukan posisi/kemiringan tangan di frame (`GesturePattern.kt`):

| Aksi robot     | Pola gestur                          | Jari terbuka                          |
|----------------|---------------------------------------|----------------------------------------|
| `IDLE`         | Kepalan tangan                        | 0 jari                                 |
| `MOVE_FORWARD` | Telunjuk saja                         | 1 (telunjuk)                           |
| `MOVE_BACKWARD`| V-sign                                | 2 (telunjuk + tengah)                  |
| `ROTATE_LEFT`  | Tiga jari                             | 3 (+ manis)                            |
| `ROTATE_RIGHT` | Empat jari tanpa jempol               | 4 (+ kelingking, jempol tertutup)      |
| `CROUCH`       | Telapak terbuka penuh                 | 5 (+ jempol)                           |

Kombinasi jari yang tidak cocok pola manapun jatuh ke `IDLE` sebagai default aman. Ambang
deteksi terkumpul di `GestureConfig` (`fingerExtensionMargin` untuk 4 jari panjang,
`thumbExtensionRatio` untuk jempol) supaya bisa dikalibrasi tanpa mengubah algoritma. Jempol
dinilai dari rasio jarak euclidean tip-vs-IP ke pergelangan (bukan perbandingan X), agar tetap
benar untuk tangan kiri maupun frame kamera depan yang di-mirror. `GestureConfidenceScorer` +
`TemporalStabilizer` (hysteresis lock, holdMs=300) menstabilkan aksi sebelum diteruskan ke
robot — detail lengkap di [`docs/FLOW.md`](./docs/FLOW.md).

## Alur end-to-end

```
[ CameraX ImageAnalysis (RGBA_8888, KEEP_ONLY_LATEST) ]
        │  Bitmap tegak (rotasi + mirror kamera depan)
        ▼
[ MediaPipe HandLandmarker  – RunningMode.LIVE_STREAM ]
        │  21 titik ternormalisasi 0..1
        ▼
[ HandLandmarkStream ]  ← satu-satunya jembatan platform → shared
        │  EMA LandmarkSmoother (anti jitter)
        ▼
[ HandGestureClassifier ] → [ TemporalStabilizer: hysteresis lock 300ms ]
        │  RobotAction stabil
        ▼
[ RobotControlViewModel ]  loop 60 FPS
        │  RobotMotionEngine.step(state, action, dt) + lerp eksponensial
        ▼
[ Robot3dCanvas / RobotCanvas ] + HUD/telemetri/overlay kerangka tangan
```

## Visualisasi robot (3D & 2D)

Render 3D produksi memakai **Filament KMP** (`io.github.erkko68.filament`, `Robot3dCanvas.kt`)
— PBR hardware-accelerated dengan directional light + shadow, bloom/FXAA, dan kamera orbit
drag; model robot dirakit langsung dari primitif Filament (`Cube`/`Cylinder`/`Sphere`/`Group`),
berjalan di Android, Desktop, dan iOS.

Pemetaan state ke transformasi 3D:

| State        | Transformasi 3D                                                        |
|--------------|------------------------------------------------------------------------|
| `positionZ`  | translasi sumbu Z dunia — maju = mendekat ke kamera (perspektif nyata)  |
| `rotationY`  | rotasi yaw di sekitar sumbu Y (badan benar-benar berputar)              |
| `scaleY`     | pinggul turun & kaki memendek; telapak tetap menapak lantai `y = 0`     |
| `walkPhase`  | rotasi engsel bahu & pinggul (ayunan lengan/kaki berlawanan fase)       |

Tombol **TAMPILAN: 3D/2D** di kiri atas berpindah ke renderer siluet 2D (`RobotCanvas`,
Compose `Canvas` murni, independen dari Filament) yang lebih ringan untuk perangkat kelas
bawah — pilihannya disimpan di `RobotUiState.renderMode`.

> **Catatan:** `presentation/render/*` (`Vec3`, `Mat4`, `Mesh`, `SoftwareRenderer`,
> `RobotMeshBuilder`, `ObjMeshLoader`) adalah rasterizer software generasi sebelum migrasi ke
> Filament — **tidak dipanggil dari mana pun** di produksi (dead code), lihat
> [`docs/AUDIT_INCOMPLETE.md` #1](./docs/AUDIT_INCOMPLETE.md#1-tiga-implementasi-renderer-robot-3d--hanya-2-yang-terpakai).

## Struktur MVVM

```
shared/src/commonMain/kotlin/com/experimental/robot/
├── domain/                                  # Model — murni Kotlin, tanpa API platform
│   ├── model/       RobotAction, RobotCommand, HaltMode, HandPoint, HandLandmarkIndex,
│   │                HandFrame, FingerState, RobotState
│   ├── gesture/      GestureConfig, GesturePattern, FingerExtensionDetector,
│   │                HandGestureClassifier, GestureConfidenceScorer, TemporalStabilizer,
│   │                EmergencyGestureDetector, LandmarkSmoother
│   ├── calibration/ CalibrationProfile, CalibrationRecorder — wizard kalibrasi
│   ├── command/     ControlSpace, CommandInterpreter, SlewRateLimiter
│   ├── safety/      SafetyController — gerbang keselamatan berlapis (lihat §5 FLOW.md)
│   └── motion/      MotionConfig, RobotMotionEngine
├── data/                                    # Sumber data
│   ├── HandTrackingRepository (kontrak), TrackerStatus, FrameRateMeter
│   └── HandLandmarkStream (implementasi + buffer DROP_OLDEST)
├── presentation/
│   ├── viewmodel/ RobotUiState, RobotControlViewModel
│   ├── ui/        RobotControlScreen, DashboardComponents, Robot3dCanvas (Filament),
│   │              RobotCanvas (2D), HandSkeletonOverlay, RobotTheme, CameraFeed (expect)
│   └── render/    rasterizer software — dead code, lihat catatan di § Visualisasi robot
└── di/            RobotGraph (service locator)

shared/src/androidMain/.../robot/
├── data/camera/   HandLandmarkerDetector   # MediaPipe + konversi ImageProxy → Bitmap
└── presentation/ui/CameraFeed.android.kt   # CameraX + izin kamera
```

- **View** (`presentation/ui`) hanya membaca `RobotUiState` dan mengirim event; tidak ada
  logika gestur di composable.
- **ViewModel** menggabungkan repository + classifier + interpreter + safety controller +
  motion engine, dan menjalankan loop kendali independen 60 FPS (`tick()`) terpisah dari FPS
  kamera — lihat [`docs/FLOW.md` §3](./docs/FLOW.md#3-dua-clock-yang-berjalan-terpisah).
- **Model/Domain** 100% common dan teruji unit test.

Platform tanpa binding kamera (Desktop/iOS pada build ini) menerbitkan
`TrackerStatus.Unsupported`, dan layar jatuh ke kontrol manual (tap kartu gestur di
`PetaGesturGrid`) sehingga layer domain serta animasi tetap dapat dicoba.

> **Catatan:** kontrol manual saat ini punya bug stuck-override (tap sekali mengunci aksi
> permanen sampai Reset/Emergency Stop ditekan) — lihat
> [`docs/AUDIT_INCOMPLETE.md` #4](./docs/AUDIT_INCOMPLETE.md#4-kontrol-manual-tap-gestur-macet-permanen--stuck-override-bug-paling-serius).
> Komponen `ManualControlPad.kt` yang sudah dibangun benar (press-and-hold) juga ada di kode
> tapi belum disambungkan ke layar manapun.

## Model MediaPipe

`androidApp/src/main/assets/hand_landmarker.task` (float16, ±7,8 MB) diunduh dari
`storage.googleapis.com/mediapipe-models/hand_landmarker/...`. Aset `.task` diset
`noCompress` di `androidApp/build.gradle.kts` agar bisa di-mmap saat runtime.

## Menjalankan

- Build APK debug: `./gradlew :androidApp:assembleDebug`
  → `androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- Desktop: `./gradlew :desktopApp:run` (kontrol manual)
- iOS: buka [/iosApp](./iosApp) di Xcode.

Izin kamera diminta saat layar pertama dibuka. Bila ditolak, aplikasi tetap berjalan dengan
kontrol manual.

## Test

- `./gradlew :shared:jvmTest` — 11 file test domain, ~125 `@Test`: classifier, confidence
  scorer, temporal stabilizer, state machine, command interpreter, control space, motion
  engine, safety controller, kalibrasi (`RobotControlViewModel` sendiri **belum** punya test —
  lihat [`docs/AUDIT_INCOMPLETE.md` #9](./docs/AUDIT_INCOMPLETE.md#9-cakupan-test-yang-kosong))
- `./gradlew :shared:testAndroidHostTest`
- `./gradlew :shared:iosSimulatorArm64Test` (perlu macOS)

## Kalibrasi cepat

| Gejala                                   | Yang diubah                                              |
|------------------------------------------|----------------------------------------------------------|
| Aksi terasa lambat berganti              | `TemporalStabilizer(holdMs = …)` di `RobotGraph`          |
| Jari kurang/lebih sensitif dibaca terbuka | `GestureConfig.fingerExtensionMargin`                     |
| Jempol (JONGKOK) sulit/mudah terpicu     | `GestureConfig.thumbExtensionRatio`                        |
| Robot terlalu cepat/lambat               | `MotionConfig.forwardSpeed`, `rotationSpeed`              |
| Landmark masih bergetar                  | `LandmarkSmoother(alpha = …)` di `HandLandmarkStream`     |

## Dokumentasi lebih lengkap

- [`docs/FLOW.md`](./docs/FLOW.md) — arsitektur & alur end-to-end detail (dua-clock, safety
  gates, kalibrasi, kontrol manual), dengan diagram.
- [`docs/AUDIT_INCOMPLETE.md`](./docs/AUDIT_INCOMPLETE.md) — 9 temuan fitur belum
  selesai/tidak sinkron (termasuk 2 bug fungsional kritis: kalibrasi tidak pernah diterapkan,
  dan kontrol manual macet permanen).
- [`docs/TEST_CASES.md`](./docs/TEST_CASES.md) — skenario manual untuk mereproduksi tiap
  temuan di atas.
