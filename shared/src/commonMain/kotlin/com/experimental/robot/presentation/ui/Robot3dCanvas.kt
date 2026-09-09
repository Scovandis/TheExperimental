package com.experimental.robot.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotState
import io.github.erkko68.filament.MaterialInstance
import io.github.erkko68.filament.compose.FilamentSceneScope
import io.github.erkko68.filament.compose.FilamentSceneView
import io.github.erkko68.filament.compose.orbitGestures
import io.github.erkko68.filament.compose.rememberOrbitCameraController
import io.github.erkko68.filament.compose.scene.AntiAliasing
import io.github.erkko68.filament.compose.scene.Bloom
import io.github.erkko68.filament.compose.scene.Direction
import io.github.erkko68.filament.compose.scene.DirectionalLight
import io.github.erkko68.filament.compose.scene.Group
import io.github.erkko68.filament.compose.scene.LightIntensity
import io.github.erkko68.filament.compose.scene.LinearColor
import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.compose.scene.PostProcessing
import io.github.erkko68.filament.compose.scene.Projection
import io.github.erkko68.filament.compose.scene.Rotation
import io.github.erkko68.filament.compose.scene.Scale
import io.github.erkko68.filament.compose.scene.ShadowConfig
import io.github.erkko68.filament.compose.scene.Shadows
import io.github.erkko68.filament.compose.scene.SkyboxSource
import io.github.erkko68.filament.compose.scene.primitives.Cube
import io.github.erkko68.filament.compose.scene.primitives.Cylinder
import io.github.erkko68.filament.compose.scene.primitives.Plane
import io.github.erkko68.filament.compose.scene.primitives.Sphere
import io.github.erkko68.filament.compose.scene.rememberCameraState
import io.github.erkko68.filament.compose.scene.rememberColorMaterialInstance
import io.github.erkko68.filament.compose.scene.rememberEmissiveMaterialInstance
import io.github.erkko68.filament.compose.scene.rememberSkyboxState
import io.github.erkko68.filament.compose.scene.rememberTransparentColorMaterialInstance
import io.github.erkko68.filament.compose.scene.toLinearColor
import kotlin.math.max
import kotlin.math.sin

/**
 * Panggung 3D Hardware-Accelerated menggunakan Google Filament KMP.
 *
 * Menampilkan:
 * - PBR Lighting dengan Directional Key Light (shadow-casting) & Fill Light
 * - Robot TV-Head oranye retro dengan artikulasi anggota tubuh (kaki, pincer)
 * - Orbit camera controller interaktif (drag untuk memutar)
 * - Post-processing Bloom & FXAA untuk efek visor glow futuristik
 * - Grid lantai dan platform sci-fi
 */
@Composable
fun Robot3dCanvas(
    state: RobotState,
    accent: Color = Color(0xFFFF9500),
    cameraDistance: Float = 5.2f,
    modifier: Modifier = Modifier,
) {
    // State kamera Filament
    val cameraState = rememberCameraState(
        initialEye = Position(0f, 1.8f, cameraDistance),
        initialTarget = Position(0f, 0.75f, 0f),
        initialProjection = Projection.Perspective(fovDegrees = 40.0),
    )

    // Controller kamera orbit interaktif (drag untuk memutar panggung)
    val orbit = rememberOrbitCameraController(cameraState)

    // Latar belakang Sci-Fi Deep Navy
    val skybox = rememberSkyboxState(
        initialSource = SkyboxSource.Color(LinearColor(0.027f, 0.051f, 0.098f))
    )

    Box(modifier = modifier) {
        FilamentSceneView(
            modifier = Modifier
                .fillMaxSize()
                .orbitGestures(orbit),
            cameraState = cameraState,
            skyboxState = skybox,
            shadows = Shadows.Pcf,
            postProcessing = PostProcessing(
                bloom = Bloom(strength = 0.35f),
                antiAliasing = AntiAliasing(fxaaEnabled = true),
            ),
        ) {
            // ── PENCAHAYAAN PBR ──────────────────────────────────────────────
            // Key light utama (membentuk bayangan lembut)
            DirectionalLight(
                direction = Direction(0.45f, -1.0f, -0.6f),
                intensity = LightIntensity.LuminousPower(95_000f),
                shadow = ShadowConfig(),
            )
            // Fill light biru sejuk dari sisi berlawanan
            DirectionalLight(
                direction = Direction(-0.6f, 0.35f, 0.7f),
                intensity = LightIntensity.LuminousPower(28_000f),
            )

            // ── MATERIAL STANDAR PBR ─────────────────────────────────────────
            // Cangkang oranye cerah utama
            val orangeMat = rememberColorMaterialInstance(
                color = LinearColor(0.98f, 0.33f, 0.11f),
                metallic = 0.12f,
                roughness = 0.35f,
            )
            // Rangka titanium / charcoal gelap
            val darkCharcoalMat = rememberColorMaterialInstance(
                color = LinearColor(0.15f, 0.17f, 0.20f),
                metallic = 0.5f,
                roughness = 0.45f,
            )
            // Engsel & capit pincer perak metalik
            val silverMetalMat = rememberColorMaterialInstance(
                color = LinearColor(0.55f, 0.59f, 0.65f),
                metallic = 0.95f,
                roughness = 0.22f,
            )
            // Sol sepatu abu-abu
            val soleMat = rememberColorMaterialInstance(
                color = LinearColor(0.28f, 0.31f, 0.35f),
                metallic = 0.3f,
                roughness = 0.6f,
            )
            // Soket mata hitam matte
            val eyeSocketMat = rememberColorMaterialInstance(
                color = LinearColor(0.04f, 0.05f, 0.07f),
                metallic = 0.0f,
                roughness = 0.85f,
            )
            // Layar mata cyan berpijar (berubah merah saat JONGKOK)
            val eyeColor = if (state.currentAction == RobotAction.CROUCH) {
                LinearColor(1.0f, 0.2f, 0.2f)
            } else {
                LinearColor(0.40f, 0.91f, 0.98f)
            }
            val eyeGlowMat = rememberEmissiveMaterialInstance(
                color = eyeColor,
                intensity = 4.0f,
            )
            // Lencana dada beraksen aksi
            val badgeGlowMat = rememberEmissiveMaterialInstance(
                color = accent.toLinearColor(),
                intensity = 2.5f,
            )
            // Cincin platform hologram cyan transparan
            val holoRingMat = rememberTransparentColorMaterialInstance(
                color = LinearColor(0.0f, 0.85f, 1.0f),
                alpha = 0.45f,
            )
            // Lantai cyber
            val groundMat = rememberColorMaterialInstance(
                color = LinearColor(0.035f, 0.065f, 0.11f),
                roughness = 0.8f,
            )

            // ── LANTAI DASAR CYBER ───────────────────────────────────────────
            Plane(
                material = groundMat,
                position = Position(0f, -0.01f, 0f),
                width = 14f,
                depth = 14f,
                receiveShadows = true,
            )

            // ── HIERARKI ROBOT & PANGGUNG BERGERAK ───────────────────────────
            val worldZ = state.positionZ / 55f
            val swing = sin(state.walkPhase)
            val hipDrop = 0.68f * (1f - state.scaleY)
            val hipY = 0.68f - hipDrop

            // Cincin Hologram Konsentris mengikuti kedalaman robot
            Group(position = Position(0f, 0.005f, worldZ)) {
                Cylinder(
                    material = holoRingMat,
                    position = Position(0f, 0f, 0f),
                    radius = 1.35f,
                    height = 0.004f,
                    segments = 36,
                    castShadows = false,
                )
                Cylinder(
                    material = holoRingMat,
                    position = Position(0f, 0.002f, 0f),
                    radius = 0.95f,
                    height = 0.004f,
                    segments = 32,
                    castShadows = false,
                )
                Cylinder(
                    material = holoRingMat,
                    position = Position(0f, 0.004f, 0f),
                    radius = 0.55f,
                    height = 0.004f,
                    segments = 28,
                    castShadows = false,
                )
            }

            // Badan Robot 3D (berotasi mengikuti yaw dan bergeser di Z)
            Group(
                position = Position(0f, 0f, worldZ),
                rotation = Rotation.axisAngle(Direction.Up, degrees = -state.rotationY),
            ) {
                // Pelvis
                Cube(
                    material = darkCharcoalMat,
                    position = Position(0f, hipY + 0.07f * state.scaleY, 0f),
                    size = 0.32f,
                    scale = Scale(1.0f, 0.45f, 0.75f),
                )

                // Torso Kapsul Oranye
                Cylinder(
                    material = orangeMat,
                    position = Position(0f, hipY + 0.32f * state.scaleY, 0f),
                    radius = 0.28f,
                    height = 0.38f * state.scaleY,
                    segments = 24,
                )

                // Lencana dada (Action Indicator)
                Cube(
                    material = badgeGlowMat,
                    position = Position(0f, hipY + 0.36f * state.scaleY, 0.28f),
                    size = 0.12f,
                    scale = Scale(1.2f, 0.35f, 0.15f),
                    castShadows = false,
                )

                // Leher
                val neckY = hipY + 0.53f * state.scaleY
                Cylinder(
                    material = darkCharcoalMat,
                    position = Position(0f, neckY, 0f),
                    radius = 0.11f,
                    height = 0.08f,
                    segments = 16,
                )

                // Kepala Kotak TV (TV-Head)
                val headY = neckY + 0.36f
                Group(position = Position(0f, headY, 0f)) {
                    // Cangkang Utama Kepala
                    Cube(
                        material = orangeMat,
                        position = Position(0f, 0f, 0f),
                        size = 0.56f,
                        scale = Scale(1.55f, 1.05f, 0.95f),
                    )

                    // Ear Pods di kiri & kanan
                    Cylinder(
                        material = darkCharcoalMat,
                        position = Position(-0.46f, 0f, 0f),
                        rotation = Rotation.axisAngle(Direction.Forward, degrees = 90f),
                        radius = 0.13f,
                        height = 0.10f,
                        segments = 20,
                    )
                    Cylinder(
                        material = darkCharcoalMat,
                        position = Position(0.46f, 0f, 0f),
                        rotation = Rotation.axisAngle(Direction.Forward, degrees = 90f),
                        radius = 0.13f,
                        height = 0.10f,
                        segments = 20,
                    )

                    // Bingkai soket layar hitam inset
                    Cube(
                        material = eyeSocketMat,
                        position = Position(0f, 0.01f, 0.27f),
                        size = 0.35f,
                        scale = Scale(1.82f, 0.92f, 0.12f),
                    )

                    // Layar Mata Cyan Kembar Berpijar
                    Cube(
                        material = eyeGlowMat,
                        position = Position(-0.14f, 0.02f, 0.29f),
                        size = 0.13f,
                        scale = Scale(1.1f, 1.15f, 0.1f),
                        castShadows = false,
                    )
                    Cube(
                        material = eyeGlowMat,
                        position = Position(0.14f, 0.02f, 0.29f),
                        size = 0.13f,
                        scale = Scale(1.1f, 1.15f, 0.1f),
                        castShadows = false,
                    )
                }

                // Lengan Artikulasi (Kiri & Kanan)
                val shoulderY = hipY + 0.44f * state.scaleY
                FilamentRobotArm(
                    isLeft = true,
                    shoulderY = shoulderY,
                    swing = swing,
                    scaleY = state.scaleY,
                    orangeMat = orangeMat,
                    charcoalMat = darkCharcoalMat,
                    metalMat = silverMetalMat,
                )
                FilamentRobotArm(
                    isLeft = false,
                    shoulderY = shoulderY,
                    swing = -swing,
                    scaleY = state.scaleY,
                    orangeMat = orangeMat,
                    charcoalMat = darkCharcoalMat,
                    metalMat = silverMetalMat,
                )

                // Kaki & Flared Boots (Kiri & Kanan)
                FilamentRobotLeg(
                    isLeft = true,
                    hipY = hipY,
                    swing = swing,
                    scaleY = state.scaleY,
                    orangeMat = orangeMat,
                    metalMat = silverMetalMat,
                    soleMat = soleMat,
                )
                FilamentRobotLeg(
                    isLeft = false,
                    hipY = hipY,
                    swing = -swing,
                    scaleY = state.scaleY,
                    orangeMat = orangeMat,
                    metalMat = silverMetalMat,
                    soleMat = soleMat,
                )
            }
        }

        // ── FLOATING STATUS BADGE ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xE60D172A))
                .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "🤖", fontSize = 13.sp)
                Text(
                    text = "ROBOT 3D",
                    color = RobotColors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x2010B981))
                    .border(1.dp, Color(0x6010B981), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                    )
                    Text(
                        text = "Filament PBR",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilamentSceneScope.FilamentRobotArm(
    isLeft: Boolean,
    shoulderY: Float,
    swing: Float,
    scaleY: Float,
    orangeMat: MaterialInstance,
    charcoalMat: MaterialInstance,
    metalMat: MaterialInstance,
) {
    val sign = if (isLeft) -1f else 1f
    val shoulderX = sign * 0.35f
    val armSwingAngle = swing * 24f

    Group(
        position = Position(shoulderX, shoulderY, 0f),
        rotation = Rotation.axisAngle(Direction.Right, degrees = armSwingAngle),
    ) {
        // Soket bahu
        Sphere(material = charcoalMat, radius = 0.08f, position = Position(0f, 0f, 0f))
        Sphere(material = orangeMat, radius = 0.065f, position = Position(sign * 0.02f, 0f, 0f))

        // Lengan atas (strut metal perak)
        Cylinder(
            material = metalMat,
            position = Position(0f, -0.10f * scaleY, 0f),
            radius = 0.04f,
            height = 0.16f * scaleY,
            segments = 16,
        )

        // Engsel siku
        Sphere(
            material = metalMat,
            position = Position(0f, -0.20f * scaleY, 0f),
            radius = 0.055f,
        )

        // Lengan bawah (oranye)
        Cylinder(
            material = orangeMat,
            position = Position(0f, -0.32f * scaleY, 0f),
            radius = 0.055f,
            height = 0.18f * scaleY,
            segments = 16,
        )

        // Dudukan capit
        Cylinder(
            material = charcoalMat,
            position = Position(0f, -0.42f * scaleY, 0f),
            radius = 0.045f,
            height = 0.04f,
            segments = 16,
        )

        // Capit pincer 3-cabang (jempol + 2 penjepit luar)
        Cube(
            material = metalMat,
            position = Position(-sign * 0.025f, -0.46f * scaleY, 0f),
            size = 0.06f,
            scale = Scale(0.3f, 1.2f, 0.4f),
        )
        Cube(
            material = metalMat,
            position = Position(sign * 0.025f, -0.46f * scaleY, 0.02f),
            size = 0.06f,
            scale = Scale(0.3f, 1.2f, 0.4f),
        )
        Cube(
            material = metalMat,
            position = Position(sign * 0.025f, -0.46f * scaleY, -0.02f),
            size = 0.06f,
            scale = Scale(0.3f, 1.2f, 0.4f),
        )
    }
}

@Composable
private fun FilamentSceneScope.FilamentRobotLeg(
    isLeft: Boolean,
    hipY: Float,
    swing: Float,
    scaleY: Float,
    orangeMat: MaterialInstance,
    metalMat: MaterialInstance,
    soleMat: MaterialInstance,
) {
    val sign = if (isLeft) -1f else 1f
    val hipX = sign * 0.17f
    val lift = max(0f, swing) * 0.12f
    val legSwingAngle = -swing * 22f

    val footZ = swing * 0.22f
    val kneeY = (hipY * 0.52f).coerceAtLeast(0.24f)

    Group(
        position = Position(hipX, 0f, 0f),
    ) {
        // Paha atas (strut metal perak berengsel)
        Group(
            position = Position(0f, hipY, 0f),
            rotation = Rotation.axisAngle(Direction.Right, degrees = legSwingAngle),
        ) {
            Sphere(material = orangeMat, radius = 0.075f, position = Position(0f, 0f, 0f))
            Cylinder(
                material = metalMat,
                position = Position(0f, -hipY * 0.24f, 0f),
                radius = 0.055f,
                height = hipY * 0.42f,
                segments = 16,
            )
            // Engsel lutut
            Cylinder(
                material = metalMat,
                position = Position(0f, -hipY * 0.46f, 0f),
                rotation = Rotation.axisAngle(Direction.Right, degrees = 90f),
                radius = 0.065f,
                height = 0.09f,
                segments = 16,
            )
        }

        // Flared boot trapesium oranye (lebih lebar di bawah)
        val bootBottomY = 0.07f + lift
        val bootHeight = kneeY - bootBottomY
        Cylinder(
            material = orangeMat,
            position = Position(0f, bootBottomY + bootHeight * 0.5f, footZ),
            radius = 0.16f,
            height = bootHeight,
            scale = Scale(1.2f, 1.0f, 1.35f),
            segments = 24,
        )

        // Sol telapak kaki penapak lantai
        Cube(
            material = soleMat,
            position = Position(0f, 0.025f + lift, footZ),
            size = 0.24f,
            scale = Scale(0.95f, 0.22f, 1.35f),
            receiveShadows = true,
        )
    }
}

