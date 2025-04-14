package com.ppam.eyemovementbellapp.analyzer


import android.content.Context
import android.graphics.ImageFormat
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.ppam.eyemovementbellapp.gesture.EyeDirection
import com.ppam.eyemovementbellapp.gesture.EyeDirectionDetector
import com.ppam.eyemovementbellapp.mediapipe.MediapipeLandmarkerHelper
import com.ppam.eyemovementbellapp.sound.SoundPlayer

class EyeGestureAnalyzer(
    private val context: Context,
) {

    private val TAG = "EyeGestureAnalyzer"
    private var lastDirection: EyeDirection = EyeDirection.NONE
    private var directionCount = 0

    fun analyze(result: FaceLandmarkerResult) {
        result.faceLandmarks().firstOrNull()?.let { landmarkList ->
            val direction = EyeDirectionDetector.detectDirection(landmarkList)

            Log.d(TAG, "Detected direction: $direction")

            if (direction != EyeDirection.NONE && direction == lastDirection) {
                directionCount++
            } else {
                directionCount = 1
            }

            lastDirection = direction

            if (directionCount == 2) {
                when (direction) {
                    EyeDirection.RIGHT -> {
                        SoundPlayer.playBellSound(context)
                        Log.d(TAG, "Right eye movement detected twice – playing sound")
                    }
                    EyeDirection.LEFT -> {
                        Log.d(TAG, "Left eye movement detected twice – placeholder action")
                    }
                    EyeDirection.UP -> {
                        Log.d(TAG, "Up eye movement detected twice – placeholder action")
                    }
                    EyeDirection.DOWN -> {
                        Log.d(TAG, "Down eye movement detected twice – placeholder action")
                    }
                    EyeDirection.NONE -> { /* Do nothing */ }
                }
                directionCount = 0 // reset
            }
        }
    }
}