package com.ppam.eyemovementbellapp.analyzer


import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark // Correct import (hopefully!)
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.ppam.eyemovementbellapp.gesture.EyeDirection
import com.ppam.eyemovementbellapp.gesture.EyeDirectionDetector
import com.ppam.eyemovementbellapp.sound.SoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
/*
class EyeGestureAnalyzer(private val context: Context) {
    private val TAG = "APK-EyeGestureAnalyzer"

    private var rightDirectionCount = 0
    private var lastDirection = EyeDirection.NONE

    private var lastGestureTime = 0L
    private val gestureCooldownMillis = 2000L

    private var lastToastTime = 0L
    private val toastCooldownMillis = 2000L

    private var frameCount = 0
    private val FRAME_WARMUP_THRESHOLD = 10 // Reduced warm-up

    fun analyze(landmarks: List<NormalizedLandmark>) {
        frameCount++
        if (frameCount < FRAME_WARMUP_THRESHOLD) {
            return
        }

        val direction = EyeDirectionDetector.detectDirection(landmarks)
        val now = System.currentTimeMillis()
        if (direction == EyeDirection.RIGHT) {
            rightDirectionCount++
            if (rightDirectionCount >= 20 && lastDirection == EyeDirection.RIGHT) {
//                toggleBell()
                SoundPlayer.startLoopingBell(context)
                Log.d(TAG, "Detected direction: $direction (Last: $lastDirection)")
                showToast("🔔 Bell Started")
            }
            lastDirection = direction
        } else if (direction == EyeDirection.LEFT || direction == EyeDirection.UP || direction == EyeDirection.DOWN) {
                if (SoundPlayer.isPlaying()) {
                    SoundPlayer.stop()
                    showToast("🔔 Bell Stopped")
                }
                lastGestureTime = now
                Log.d(TAG, "Detected direction: $direction (Last: $lastDirection)")

            rightDirectionCount = 0
        }
        lastDirection = direction
    }

    private fun toggleBell() {
        if (SoundPlayer.isPlaying()) {
            SoundPlayer.stop()
            Log.d(TAG, "Stopping bell")
        } else {
            SoundPlayer.startLoopingBell(context)
            Log.d(TAG, "Starting bell looping")
        }
    }

    private fun showToast(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastToastTime > toastCooldownMillis) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            lastToastTime = now
        }
    }

    fun shutdown() {
        SoundPlayer.stop()
    }
}

 */
class EyeGestureAnalyzer(private val context: Context) {
    private val TAG = "APK-EyeGestureAnalyzer"

    private var rightDirectionCount = 0
    private var lastDirection = EyeDirection.NONE

    private var lastToastTime = 0L
    private val toastCooldownMillis = 2000L

    private var frameCount = 0
    private val FRAME_WARMUP_THRESHOLD = 10

    private var lastCenterX = 0f
    private var averageEyeSpeed = 0f
    private val SPEED_SMOOTHING_ALPHA = 0.2f

    // 👇 Add this MutableState
    private val _currentDirection = MutableStateFlow(EyeDirection.NONE)
    val currentDirection: StateFlow<EyeDirection> = _currentDirection

    fun analyze(landmarks: List<NormalizedLandmark>) {
        frameCount++
        if (frameCount < FRAME_WARMUP_THRESHOLD) {
            return
        }

        val direction = EyeDirectionDetector.detectDirection(landmarks)
        _currentDirection.value = direction // 👈 Update live direction!


        val centerX = EyeDirectionDetector.getEyeCenterX(landmarks)
        if (centerX != null) {
            val speedX = kotlin.math.abs(centerX - lastCenterX)
            averageEyeSpeed = SPEED_SMOOTHING_ALPHA * speedX + (1 - SPEED_SMOOTHING_ALPHA) * averageEyeSpeed
            lastCenterX = centerX
        }

        val dynamicThreshold = when {
            averageEyeSpeed > 0.008f -> 10
            averageEyeSpeed > 0.004f -> 15
            else -> 20
        }

        if (direction == EyeDirection.RIGHT) {
            rightDirectionCount++
            if (rightDirectionCount >= dynamicThreshold && lastDirection == EyeDirection.RIGHT) {
                if (!SoundPlayer.isPlaying()) {
                    SoundPlayer.startLoopingBell(context)
                }
                Log.d(TAG, "Detected direction: $direction (Last: $lastDirection)")
                showToast("🔔 Bell Started")
            }
        } else if (direction == EyeDirection.LEFT || direction == EyeDirection.UP || direction == EyeDirection.DOWN || direction == EyeDirection.CENTER) {
            if (SoundPlayer.isPlaying()) {
                SoundPlayer.stop()
                showToast("🔔 Bell Stopped")
            }
            rightDirectionCount = 0
        }
        lastDirection = direction
    }

    private fun showToast(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastToastTime > toastCooldownMillis) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            lastToastTime = now
        }
    }

    fun shutdown() {
        SoundPlayer.stop()
    }
}