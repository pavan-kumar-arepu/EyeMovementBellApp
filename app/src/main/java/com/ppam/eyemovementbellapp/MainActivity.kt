package com.ppam.eyemovementbellapp

import android.os.Bundle


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previewView = PreviewView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(previewView)

        checkPermissionsAndStartCamera()

        // Initialize FaceLandmarkerHelper
        faceLandmarkerHelper = MediapipeLandmarkerHelper(this)
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

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            // Set up your analyzer here
            val analyzer = EyeGestureAnalyzer(this)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                        // Convert the CameraX ImageProxy to a format that MediaPipe can handle
//                        val mediaPipeImage = convertToMediaPipeImage(image)
                        analyzer.analyze(image)
                        image.close()  // Don't forget to close the ImageProxy!
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

//    private fun convertToMediaPipeImage(imageProxy: ImageProxy): MPImage {
//        // Extract the YUV data from ImageProxy (depending on your use case)
//        val yuvImage = YuvImage(imageProxy.planes[0].buffer, imageProxy.format, imageProxy.width, imageProxy.height, null)
//
//        // Convert the YUV data to JPEG (or another suitable format for MediaPipe)
//        val byteArrayOutputStream = ByteArrayOutputStream()
//        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, byteArrayOutputStream)
//
//        // Convert the byte data to MPImage (this might require a custom method depending on MediaPipe's input format)
//        return MPImage.createFromByteArray(byteArrayOutputStream.toByteArray())
//    }

    override fun onDestroy() {
        super.onDestroy()
        faceLandmarkerHelper.close() // Close MediaPipe helper when activity is destroyed
    }
}