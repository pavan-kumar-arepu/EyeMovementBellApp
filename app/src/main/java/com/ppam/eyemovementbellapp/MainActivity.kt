package com.ppam.eyemovementbellapp

import android.os.Bundle

/*
import android.graphics.Rect
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.YuvImage
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.MPImage
import com.ppam.eyemovementbellapp.analyzer.EyeGestureAnalyzer
import com.ppam.eyemovementbellapp.mediapipe.MediapipeLandmarkerHelper
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView

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

    private lateinit var faceLandmarkerHelper: MediapipeLandmarkerHelper
    private lateinit var eyeGestureAnalyzer: EyeGestureAnalyzer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previewView = PreviewView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(previewView)

        eyeGestureAnalyzer = EyeGestureAnalyzer(this)

        // ✅ Correct initialization with result listener
        faceLandmarkerHelper = MediapipeLandmarkerHelper(this) { result ->
            result?.let {
                eyeGestureAnalyzer.analyze(it)
            }
        }

        checkPermissionsAndStartCamera()
    }

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
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT) // Use this if you want to use the front camera.
                //.requireLensFacing(CameraSelector.LENS_FACING_BACK) // Use this if you want to use the back camera.
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        val result = faceLandmarkerHelper.detectFaceLandmarks(imageProxy)
                        result?.let {
                            eyeGestureAnalyzer.analyze(it)
                        }
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


    override fun onDestroy() {
        super.onDestroy()
        faceLandmarkerHelper.close() // Close MediaPipe helper when activity is destroyed
    }
}

 */
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
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.ppam.eyemovementbellapp.analyzer.EyeGestureAnalyzer
import com.ppam.eyemovementbellapp.mediapipe.MediapipeLandmarkerHelper
import java.util.concurrent.Executors
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.ppam.eyemovementbellapp.sound.SoundPlayer.startLoopingBell
import java.util.LinkedList

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
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
    private lateinit var faceLandmarkerHelper: MediapipeLandmarkerHelper
    private lateinit var eyeGestureAnalyzer: EyeGestureAnalyzer
    private lateinit var cameraExecutor: java.util.concurrent.ExecutorService // Correct initialization
    private var mediaPlayer: MediaPlayer? = null

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

        mediaPlayer = MediaPlayer.create(this, R.raw.templebells) // Initialize MediaPlayer

        eyeGestureAnalyzer = EyeGestureAnalyzer(this) { startLoopingBell(this) } // Initialize with callback

        // ✅ Correct initialization with result listener
        faceLandmarkerHelper = MediapipeLandmarkerHelper(this) { result ->
            result.let {
                it.faceLandmarks().firstOrNull()?.let { landmarks: List<NormalizedLandmark> ->
                    eyeGestureAnalyzer.analyze(landmarks)
                }
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor() // Initialize cameraExecutor

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
                        // ✅ Use the correct function for live stream and pass the ImageProxy
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

//    private fun playBellSound() {
//        mediaPlayer?.start()
//    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // Correct shutdown call
        mediaPlayer?.release()
        mediaPlayer = null
        faceLandmarkerHelper.close() // Close MediaPipe helper when activity is destroyed
    }
}