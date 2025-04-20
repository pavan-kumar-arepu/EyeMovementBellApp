package com.ppam.eyemovementbellapp.gesture


import android.util.Log
//import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.collections.getOrNull
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
object EyeDirectionDetector {
    private const val TAG = "EyeDirectionDetector"

    private const val LEFT_EYE_INNER_CORNER = 133
    private const val LEFT_EYE_OUTER_CORNER = 159
    private const val RIGHT_EYE_INNER_CORNER = 362
    private const val RIGHT_EYE_OUTER_CORNER = 386

    private const val NOSE_TIP = 4

    private val eyeXHistory = mutableListOf<Float>()

    // Adjusted thresholds for more stable detection
    private const val horizontalThresholdRight = 0.07f
    private const val horizontalThresholdLeft = -0.07f

    // Warm-up logic to prevent early false positives
    private var frameCount = 0
    private const val FRAME_WARMUP_THRESHOLD = 15

    // Debounce to prevent repeated detection too quickly
    private var lastDirectionDetectedTime = 0L
    private const val DEBOUNCE_INTERVAL = 1000L // 1 second

    fun detectDirection(landmarks: List<NormalizedLandmark>): EyeDirection {

        frameCount++
        if (frameCount < FRAME_WARMUP_THRESHOLD) {
            Log.d(TAG, "Warming up: Skipping frame $frameCount")
            return EyeDirection.NONE
        }

        val leftEyeIn = landmarks.getOrNull(LEFT_EYE_INNER_CORNER)
        val leftEyeOut = landmarks.getOrNull(LEFT_EYE_OUTER_CORNER)
        val rightEyeIn = landmarks.getOrNull(RIGHT_EYE_INNER_CORNER)
        val rightEyeOut = landmarks.getOrNull(RIGHT_EYE_OUTER_CORNER)

        if (leftEyeIn == null || leftEyeOut == null || rightEyeIn == null || rightEyeOut == null) {
            Log.w(TAG, "Eye landmarks missing.")
            return EyeDirection.NONE
        }

        val leftEyeX = (leftEyeIn.x() + leftEyeOut.x()) / 2f
        val rightEyeX = (rightEyeIn.x() + rightEyeOut.x()) / 2f
        val eyeCenterX = (leftEyeX + rightEyeX) / 2f

        // Smooth the X value to avoid jittery movements
        val smoothedX = smoothValue(eyeXHistory, eyeCenterX, 5)

        // Calculate horizontal difference from the center (0.5)
        val horizontalDiffFromCenter = smoothedX - 0.5f

        val direction = when {
            horizontalDiffFromCenter > horizontalThresholdRight -> EyeDirection.LEFT
            horizontalDiffFromCenter < horizontalThresholdLeft -> EyeDirection.RIGHT
            else -> EyeDirection.NONE
        }

        // Apply debounce check before returning direction
        val now = System.currentTimeMillis()
        return if (direction != EyeDirection.NONE && now - lastDirectionDetectedTime > DEBOUNCE_INTERVAL) {
            lastDirectionDetectedTime = now
            Log.d(TAG, "Smoothed EyeCenterX=$smoothedX, DiffFromCenter=$horizontalDiffFromCenter, Direction=$direction")
            direction
        } else {
            EyeDirection.NONE
        }
    }

    private fun smoothValue(history: MutableList<Float>, newValue: Float, maxSize: Int): Float {
        history.add(newValue)
        if (history.size > maxSize) history.removeAt(0)
        return history.average().toFloat()
    }

    fun isEyeOpen(landmarks: List<NormalizedLandmark>): Boolean {
        val leftTop = landmarks.getOrNull(159)?.y() ?: return false
        val leftBottom = landmarks.getOrNull(145)?.y() ?: return false
        val rightTop = landmarks.getOrNull(386)?.y() ?: return false
        val rightBottom = landmarks.getOrNull(374)?.y() ?: return false

        val leftEyeOpen = abs(leftTop - leftBottom) > 0.015f
        val rightEyeOpen = abs(rightTop - rightBottom) > 0.015f

        return leftEyeOpen && rightEyeOpen
    }
}

enum class EyeDirection {
    LEFT, RIGHT, NONE
}