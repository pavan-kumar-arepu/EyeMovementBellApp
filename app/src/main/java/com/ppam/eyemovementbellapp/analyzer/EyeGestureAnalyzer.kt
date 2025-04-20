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
//class EyeGestureAnalyzer(
//    private val context: Context,
//    private val onBellSoundRequested: () -> Unit
//) {
//    private val TAG = "EyeGestureAnalyzer"
//
//    private var rightDirectionCount = 0
//    private var lastDirection = EyeDirection.NONE
//    private var isBellPlaying = false
//
//    private var frameCount = 0
//    private val ignoreInitialFrames = 15
//
//    private var lastGestureTime = 0L
//    private val gestureCooldownMillis = 2000L
//
//    private var lastToastTime = 0L
//
//    private var rightGestureStartTime = 0L
//
//    fun analyze(landmarks: List<NormalizedLandmark>) {
//        frameCount++
//        if (frameCount <= ignoreInitialFrames) return
//        if (!EyeDirectionDetector.isEyeOpen(landmarks)) return
//
//        val direction = EyeDirectionDetector.detectDirection(landmarks)
//        Log.d(TAG, "Direction: $direction, RightCount: $rightDirectionCount")
//
//        val now = System.currentTimeMillis()
//
//        if (direction == EyeDirection.RIGHT) {
//            if (lastDirection != EyeDirection.RIGHT) {
//                rightDirectionCount++
//                rightGestureStartTime = now
//            } else if (now - rightGestureStartTime > 1000) {
//                rightDirectionCount = 1 // Too slow
//            }
//
//            lastDirection = EyeDirection.RIGHT
//        } else {
//            lastDirection = direction
//        }
//
//        if (rightDirectionCount == 2) {
//            if (now - lastGestureTime > gestureCooldownMillis) {
//                lastGestureTime = now
//                isBellPlaying = !isBellPlaying
//                if (isBellPlaying) {
//                    SoundPlayer.startLoopingBell(context)
//                } else {
//                    SoundPlayer.stop()
//                }
//                val msg = if (isBellPlaying) "Bell started" else "Bell stopped"
//                showToast("Double RIGHT detected → $msg")
//            }
//            rightDirectionCount = 0
//        }
//    }
//
//    private fun showToast(message: String) {
//        val now = System.currentTimeMillis()
//        if (now - lastToastTime > 2000) {
//            Handler(Looper.getMainLooper()).post {
//                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
//            }
//            lastToastTime = now
//        }
//    }
//}


class EyeGestureAnalyzer(
    private val context: Context,
    private val onBellSoundRequested: () -> Unit
) {
    private val TAG = "EyeGestureAnalyzer"

    private var rightDirectionCount = 0
    private var lastDirection = EyeDirection.NONE

    private var lastGestureTime = 0L
    private val gestureCooldownMillis = 500L // Faster cooldown for responsiveness

    private var lastToastTime = 0L

    fun analyze(landmarks: List<NormalizedLandmark>) {
        // Ensure eye is open before analyzing
//        if (!EyeDirectionDetector.isEyeOpen(landmarks)) return

        val direction = EyeDirectionDetector.detectDirection(landmarks)
        val now = System.currentTimeMillis()

        Log.d("APK: EyeGestureAnalyzer", "Direction: $direction")
        // Reset the count if the direction is not right
        if (direction == EyeDirection.RIGHT) {

            rightDirectionCount++
            Log.d("APK: EyeGestureAnalyzer", "Direction: $direction, RightCount: $rightDirectionCount")

            showToast("Single RIGHT detected")

        } else {
            rightDirectionCount = 0
        }

        // Trigger the bell after detecting 2 consecutive right movements
        if (rightDirectionCount == 2) {
            if (now - lastGestureTime > gestureCooldownMillis) {
                // Toggle the bell sound
                onBellSoundRequested.invoke()
                showToast("Double RIGHT detected ")

                // Update the time for cooldown
                lastGestureTime = now

                // Log and toast message
                Log.d(TAG, "Double RIGHT detected → Bell triggered")
                showToast("Double RIGHT detected → Bell triggered")
            }
            rightDirectionCount = 0 // Reset after triggering the gesture
        }
    }

    // Method to show toast with cooldown to avoid frequent toasts
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