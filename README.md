# Gesture Robot Controller (KMP + MVVM)

Aplikasi Kotlin Multiplatform (Android / Desktop / iOS) yang mengendalikan animasi robot
prosedural memakai gestur tangan. Pipeline Android: **CameraX → MediaPipe Hand Landmarker →
Gesture Classifier → Debouncer → Motion Engine → Compose Canvas**.

## Peta gestur

| Aksi robot     | Pola gestur                        | Logika landmark                                                   |
|----------------|------------------------------------|-------------------------------------------------------------------|
| `IDLE`         | Kepalan tangan                     | Semua jari tertutup (tip di bawah PIP)                            |
| `MOVE_FORWARD` | Telapak terbuka di area atas       | ≥4 jari panjang terbuka & `wrist.y ≤ 0.65`                        |
| `MOVE_BACKWARD`| Telunjuk menunjuk ke bawah         | Hanya telunjuk terbuka & `indexTip.y > wrist.y + 0.03`            |
| `ROTATE_LEFT`  | V-sign condong ke kiri             | Telunjuk+tengah terbuka & `indexTip.x < wrist.x - 0.05`           |
| `ROTATE_RIGHT` | V-sign condong ke kanan            | Telunjuk+tengah terbuka & `indexTip.x > wrist.x + 0.05`           |
| `CROUCH`       | Telapak terbuka didorong ke bawah  | ≥4 jari panjang terbuka & `wrist.y > 0.65`                        |

Ambang batas terkumpul di `GestureConfig` supaya bisa dikalibrasi tanpa mengubah algoritma.
Jempol dinilai dari jarak euclidean tip-vs-IP ke pergelangan (bukan perbandingan X), agar
tetap benar untuk tangan kiri maupun frame kamera depan yang di-mirror.

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
[ HandGestureClassifier ] → [ GestureDebouncer: tahan 4 frame ]
        │  RobotAction stabil
        ▼
[ RobotControlViewModel ]  loop 60 FPS
        │  RobotMotionEngine.step(state, action, dt) + lerp eksponensial
        ▼
[ Robot3dCanvas / RobotCanvas ] + HUD/telemetri/overlay kerangka tangan
```

## Visualisasi robot (3D & 2D)

Robot dirender **3D** memakai renderer software sendiri di `commonMain` — tanpa engine
grafis tambahan, sehingga hasilnya identik di Android, Desktop, dan iOS:

```
presentation/render/
├── Vec3, Mat4            # aljabar vektor & matriks affine 4x4
├── Mesh (Face, box, groundQuad)
│                         # model robot = ±20 balok; winding CCW menghadap luar
├── Camera                # kamera orbit (target, distance, azimuth, pitch, focal)
├── SoftwareRenderer      # proyeksi perspektif → back-face culling →
│                         # painter's algorithm → flat shading Lambert
└── RobotMeshBuilder      # menyusun robot + rig sederhana dari RobotState
```

Pemetaan state ke 3D jadi transformasi sesungguhnya:

| State        | Transformasi 3D                                                        |
|--------------|------------------------------------------------------------------------|
| `positionZ`  | translasi sumbu Z dunia — maju = mendekat ke kamera (perspektif nyata)  |
| `rotationY`  | rotasi yaw di sekitar sumbu Y (badan benar-benar berputar)              |
| `scaleY`     | pinggul turun & kaki memendek; telapak tetap menapak lantai `y = 0`     |
| `walkPhase`  | rotasi engsel bahu & pinggul (ayunan lengan/kaki berlawanan fase)       |

Kamera orbit selalu diarahkan ke `target`, jadi robot tetap di tengah panggung pada
pitch/azimut apa pun; azimut default `-24°` memberi sudut pandang 3/4. Tombol
**TAMPILAN: 3D/2D** di kiri atas berpindah ke renderer siluet 2D (`RobotCanvas`) yang
lebih ringan untuk perangkat kelas bawah — pilihannya disimpan di `RobotUiState.renderMode`.

## Struktur MVVM

```
shared/src/commonMain/kotlin/com/experimental/robot/
├── domain/                                  # Model — murni Kotlin, tanpa API platform
│   ├── model/     RobotAction, HandPoint, HandLandmarkIndex, HandFrame,
│   │              FingerState, RobotState
│   ├── gesture/   GestureConfig, FingerExtensionDetector, GestureClassifier,
│   │              HandGestureClassifier, GestureDebouncer, LandmarkSmoother
│   └── motion/    MotionConfig, RobotMotionEngine
├── data/                                    # Sumber data
│   ├── HandTrackingRepository (kontrak), TrackerStatus
│   └── HandLandmarkStream (implementasi + buffer DROP_OLDEST)
├── presentation/
│   ├── viewmodel/ RobotUiState, RobotControlViewModel
│   └── ui/        RobotControlScreen, RobotCanvas, HandSkeletonOverlay,
│                  GestureHud, ManualControlPad, RobotTheme, CameraFeed (expect)
└── di/            RobotGraph (service locator)

shared/src/androidMain/.../robot/
├── data/camera/   HandLandmarkerDetector   # MediaPipe + konversi ImageProxy → Bitmap
└── presentation/ui/CameraFeed.android.kt   # CameraX + izin kamera
```

- **View** (`presentation/ui`) hanya membaca `RobotUiState` dan mengirim event; tidak ada
  logika gestur di composable.
- **ViewModel** menggabungkan repository + classifier + debouncer + motion engine, dan
  menjalankan loop gerak berbasis `deltaTime` sehingga robot bergerak kontinu selama gestur
  ditahan (bukan sekali lompat per frame kamera).
- **Model/Domain** 100% common dan teruji unit test.

Platform tanpa binding kamera (Desktop/iOS pada build ini) menerbitkan
`TrackerStatus.Unsupported`, dan layar otomatis menampilkan `ManualControlPad` sehingga
layer domain serta animasi tetap dapat dicoba.

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

- `./gradlew :shared:jvmTest` — 20 test: classifier (9), debouncer (4), motion engine (7)
- `./gradlew :shared:testAndroidHostTest`
- `./gradlew :shared:iosSimulatorArm64Test` (perlu macOS)

## Kalibrasi cepat

| Gejala                                   | Yang diubah                                              |
|------------------------------------------|----------------------------------------------------------|
| Aksi terasa lambat berganti              | `GestureDebouncer(framesToConfirm = …)` di `RobotGraph`   |
| Jari kurang/lebih sensitif dibaca terbuka | `GestureConfig.fingerExtensionMargin`                     |
| Jempol (JONGKOK) sulit/mudah terpicu     | `GestureConfig.thumbExtensionRatio`                        |
| Robot terlalu cepat/lambat               | `MotionConfig.forwardSpeed`, `rotationSpeed`              |
| Landmark masih bergetar                  | `LandmarkSmoother(alpha = …)` di `HandLandmarkStream`     |
