package com.experimental.robot.data

import com.experimental.robot.domain.model.HandFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sumber data landmark tangan untuk layer presentation.
 *
 * Emisi bernilai `null` berarti tidak ada tangan pada frame tersebut.
 */
interface HandTrackingRepository {
    val handFrames: Flow<HandFrame?>
    val status: StateFlow<TrackerStatus>
}
