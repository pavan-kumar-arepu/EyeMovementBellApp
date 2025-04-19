package com.ppam.eyemovementbellapp.analyzer


import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark // Correct import (hopefully!)
import com.ppam.eyemovementbellapp.gesture.EyeDirection
import com.ppam.eyemovementbellapp.gesture.EyeDirectionDetector
import com.ppam.eyemovementbellapp.sound.SoundPlayer
class EyeGestureAnalyzer(
    private val context: Context,
    private val onBellSoundRequested: () -> Unit
) {
    private val TAG = "EyeGestureAnalyzer"

    private var rightDirectionCount = 0
    private var lastDirection = EyeDirection.NONE
    private var isBellPlaying = false

    private var frameCount = 0
    private val ignoreInitialFrames = 15

    private var lastGestureTime = 0L
    private val gestureCooldownMillis = 2000L

    private var lastToastTime = 0L

    fun analyze(landmarks: List<NormalizedLandmark>) {
        frameCount++
        if (frameCount <= ignoreInitialFrames) return

        if (!EyeDirectionDetector.isEyeOpen(landmarks)) return

        val direction = EyeDirectionDetector.detectDirection(landmarks)
        Log.d(TAG, "Direction: $direction, RightCount: $rightDirectionCount, Last: $lastDirection")

        if (direction == EyeDirection.RIGHT) {
            if (lastDirection == EyeDirection.RIGHT) {
                rightDirectionCount++
            } else {
                rightDirectionCount = 1
            }
            lastDirection = EyeDirection.RIGHT
        } else {
            rightDirectionCount = 0
            lastDirection = direction
        }

        if (rightDirectionCount == 2) {
            val now = System.currentTimeMillis()
            if (now - lastGestureTime > gestureCooldownMillis) {
                isBellPlaying = !isBellPlaying
                lastGestureTime = now
                val message = if (isBellPlaying) "Bell started" else "Bell stopped"
                onBellSoundRequested.invoke()
                Log.d(TAG, "Double RIGHT detected → $message")
                showToast("Double RIGHT detected → $message")
            }
            rightDirectionCount = 0 // Reset after the gesture
        }
    }

    private fun showToast(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastToastTime > 2000) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            lastToastTime = now
        }
    }
}
