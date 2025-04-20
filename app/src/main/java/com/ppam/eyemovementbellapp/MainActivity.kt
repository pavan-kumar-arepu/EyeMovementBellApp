package com.ppam.eyemovementbellapp

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.ppam.eyemovementbellapp.analyzer.EyeGestureAnalyzer
import com.ppam.eyemovementbellapp.mediapipe.MediapipeLandmarkerHelper
import java.util.concurrent.Executors
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var faceLandmarkerHelper: MediapipeLandmarkerHelper
    private lateinit var eyeGestureAnalyzer: EyeGestureAnalyzer
    private lateinit var cameraExecutor: java.util.concurrent.ExecutorService
    private var mediaPlayer: MediaPlayer? = null
    private var isBellPlaying = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewView = PreviewView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(previewView)

        mediaPlayer = MediaPlayer.create(this, R.raw.templebells)

        // Initialize analyzer with callback
        eyeGestureAnalyzer = EyeGestureAnalyzer(this) { toggleBell() }

        // MediaPipe FaceLandmarker setup
        faceLandmarkerHelper = MediapipeLandmarkerHelper(this) { result ->
            result.let {
                it.faceLandmarks().firstOrNull()?.let { landmarks: List<NormalizedLandmark> ->
                    eyeGestureAnalyzer.analyze(landmarks)
                }
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        checkPermissionsAndStartCamera()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun checkPermissionsAndStartCamera() {
        val permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.FOREGROUND_SERVICE
        )
        val notGranted = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        faceLandmarkerHelper.detect(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("Camera", "Error binding camera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleBell() {
        if (isBellPlaying) {
            mediaPlayer?.pause()
            mediaPlayer?.seekTo(0)
            isBellPlaying = false
        } else {
            mediaPlayer?.start()
            isBellPlaying = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        mediaPlayer?.release()
        mediaPlayer = null
        faceLandmarkerHelper.close()
    }
}