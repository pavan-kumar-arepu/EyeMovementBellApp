package com.ppam.eyemovementbellapp.gesture


import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

object EyeDirectionDetector {

    fun detectDirection(landmarks: List<NormalizedLandmark>): EyeDirection {
        val leftEye: NormalizedLandmark? = landmarks.getOrNull(33)
        val rightEye: NormalizedLandmark? = landmarks.getOrNull(263)

        return if (leftEye != null && rightEye != null) {
            val eyeCenterX = (leftEye.x() + rightEye.x()) / 2.0f

            when {
                eyeCenterX < 0.4f -> EyeDirection.LEFT
                eyeCenterX > 0.6f -> EyeDirection.RIGHT
                else -> EyeDirection.NONE
            }
        } else {
            EyeDirection.NONE
        }
    }
}