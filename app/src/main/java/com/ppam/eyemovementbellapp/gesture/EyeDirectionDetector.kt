package com.ppam.eyemovementbellapp.gesture


import android.util.Log
import kotlin.collections.getOrNull
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark


object EyeDirectionDetector {
    private const val TAG = "APK-EyeDirectionDetector"

    // Correct MediaPipe Face Mesh landmark indices
    private const val LEFT_IRIS_CENTER = 468
    private const val RIGHT_IRIS_CENTER = 473
    private const val LEFT_EYE_INNER_CORNER = 362
    private const val LEFT_EYE_OUTER_CORNER = 263
    private const val RIGHT_EYE_INNER_CORNER = 133
    private const val RIGHT_EYE_OUTER_CORNER = 33

    private var smoothedX: Float? = null
    private var smoothedY: Float? = null
    private const val SMOOTHING_ALPHA = 0.5f

    private var minX = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var minY = Float.MAX_VALUE
    private var maxY = Float.MIN_VALUE

    private var frameCount = 0
    private const val FRAME_WARMUP_THRESHOLD = 5

    fun resetCalibration() {
        minX = Float.MAX_VALUE
        maxX = Float.MIN_VALUE
        minY = Float.MAX_VALUE
        maxY = Float.MIN_VALUE
        frameCount = 0
        smoothedX = null
        smoothedY = null
        Log.d(TAG, "Calibration reset")
    }

    fun detectDirection(landmarks: List<NormalizedLandmark>): EyeDirection {
        frameCount++
        if (frameCount < FRAME_WARMUP_THRESHOLD) {
            Log.d(TAG, "Warm-up frame: $frameCount")
            return EyeDirection.NONE
        }

        val leftIris = landmarks.getOrNull(LEFT_IRIS_CENTER)
        val rightIris = landmarks.getOrNull(RIGHT_IRIS_CENTER)
        val leftInner = landmarks.getOrNull(LEFT_EYE_INNER_CORNER)
        val leftOuter = landmarks.getOrNull(LEFT_EYE_OUTER_CORNER)
        val rightInner = landmarks.getOrNull(RIGHT_EYE_INNER_CORNER)
        val rightOuter = landmarks.getOrNull(RIGHT_EYE_OUTER_CORNER)

        if (leftIris == null || rightIris == null || leftInner == null || leftOuter == null || rightInner == null || rightOuter == null) {
            Log.w(TAG, "Missing eye landmarks")
            return EyeDirection.NONE
        }

        // Calculate eye center
        val centerX = (
                leftIris.x() + rightIris.x() +
                        leftInner.x() + leftOuter.x() +
                        rightInner.x() + rightOuter.x()
                ) / 6f

        val centerY = (
                leftIris.y() + rightIris.y() +
                        leftInner.y() + leftOuter.y() +
                        rightInner.y() + rightOuter.y()
                ) / 6f

        // Update min/max ranges
        minX = minOf(minX, centerX)
        maxX = maxOf(maxX, centerX)
        minY = minOf(minY, centerY)
        maxY = maxOf(maxY, centerY)

        // Smooth the movement
        smoothedX = smoothedX?.let { SMOOTHING_ALPHA * centerX + (1 - SMOOTHING_ALPHA) * it } ?: centerX
        smoothedY = smoothedY?.let { SMOOTHING_ALPHA * centerY + (1 - SMOOTHING_ALPHA) * it } ?: centerY

        val smoothX = smoothedX!!
        val smoothY = smoothedY!!

        val rangeX = maxX - minX
        val rangeY = maxY - minY

        if (rangeX == 0f || rangeY == 0f) {
            Log.d(TAG, "Range too small, returning CENTER")
            return EyeDirection.CENTER
        }

        val leftThreshold = minX + 0.3f * rangeX
        val rightThreshold = minX + 0.7f * rangeX
        val upThreshold = minY + 0.3f * rangeY
        val downThreshold = minY + 0.7f * rangeY

        val direction = when {
            smoothX > rightThreshold -> EyeDirection.RIGHT
            smoothX < leftThreshold -> EyeDirection.LEFT
            smoothY < upThreshold -> EyeDirection.UP
            smoothY > downThreshold -> EyeDirection.DOWN
            else -> EyeDirection.CENTER
        }

        Log.d(TAG, "Eye center (X=$smoothX, Y=$smoothY), thresholds: [$leftThreshold-$rightThreshold]x[$upThreshold-$downThreshold], direction=$direction")

        return direction
    }
}