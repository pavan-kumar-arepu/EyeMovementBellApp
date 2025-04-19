package com.ppam.eyemovementbellapp.gesture


import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.collections.getOrNull

object EyeDirectionDetector {
    private const val TAG = "EyeDirectionDetector"

    private const val LEFT_EYE_INNER_CORNER = 133
    private const val LEFT_EYE_OUTER_CORNER = 159
    private const val RIGHT_EYE_INNER_CORNER = 362
    private const val RIGHT_EYE_OUTER_CORNER = 386
    private const val NOSE_TIP = 4

    private val eyeXHistory = mutableListOf<Float>()

    fun detectDirection(landmarks: List<NormalizedLandmark>): EyeDirection {
        val leftEyeIn = landmarks.getOrNull(LEFT_EYE_INNER_CORNER)
        val leftEyeOut = landmarks.getOrNull(LEFT_EYE_OUTER_CORNER)
        val rightEyeIn = landmarks.getOrNull(RIGHT_EYE_INNER_CORNER)
        val rightEyeOut = landmarks.getOrNull(RIGHT_EYE_OUTER_CORNER)
        val noseTip = landmarks.getOrNull(NOSE_TIP)

        val leftEyeX = (leftEyeIn!!.x() + leftEyeOut!!.x()) / 2f
        val rightEyeX = (rightEyeIn!!.x() + rightEyeOut!!.x()) / 2f
        val eyeCenterX = (leftEyeX + rightEyeX) / 2f

        val smoothedX = smoothValue(eyeXHistory, eyeCenterX, 5) // Increased smoothing

        val horizontalDiffFromCenter = smoothedX - 0.5f // Assuming initial center is around 0.5

        val horizontalThresholdRight = 0.03f // Adjust
        val horizontalThresholdLeft = -0.03f // Adjust

        Log.d(TAG, "Smoothed EyeCenterX=$smoothedX, DiffFromCenter=$horizontalDiffFromCenter")

        val direction = when {
            horizontalDiffFromCenter > horizontalThresholdRight -> EyeDirection.RIGHT
            horizontalDiffFromCenter < horizontalThresholdLeft -> EyeDirection.LEFT
            else -> EyeDirection.NONE
        }

        Log.d(TAG, "Final Eye Direction: $direction")
        return direction
    }

    private fun smoothValue(history: MutableList<Float>, newValue: Float, maxSize: Int = 3): Float {
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