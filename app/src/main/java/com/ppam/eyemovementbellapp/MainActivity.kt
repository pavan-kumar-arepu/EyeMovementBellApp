package com.ppam.eyemovementbellapp
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ppam.eyemovementbellapp.analyzer.EyeGestureAnalyzer
import java.util.concurrent.Executors
import com.ppam.eyemovementbellapp.analyzer.EyeGestureFloatingLabel
import com.ppam.eyemovementbellapp.gesture.EyeDirectionDetector
import com.ppam.eyemovementbellapp.mediapipe.MediapipeLandmarkerHelper
import androidx.compose.ui.Modifier

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var eyeGestureAnalyzer: EyeGestureAnalyzer
    private lateinit var faceLandmarkerHelper: MediapipeLandmarkerHelper
    private lateinit var cameraExecutor: java.util.concurrent.ExecutorService

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required!", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize your analyzers
        eyeGestureAnalyzer = EyeGestureAnalyzer(this)
        faceLandmarkerHelper = MediapipeLandmarkerHelper(this) { result ->
            if (result.faceLandmarks().isNullOrEmpty()) {
                EyeDirectionDetector.resetCalibration()
            } else {
                val landmarks = result.faceLandmarks().firstOrNull()
                if (landmarks != null) {
                    eyeGestureAnalyzer.analyze(landmarks)
                }
            }
        }
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup UI
        setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context ->
                            previewView = PreviewView(context).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            }
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Floating eye direction label
                    EyeGestureFloatingLabel(eyeGestureAnalyzer)
                }
            }
        }

        checkPermissionsAndStartCamera()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun checkPermissionsAndStartCamera() {
        val permissions = listOf(
            Manifest.permission.CAMERA
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

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        faceLandmarkerHelper.detect(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceLandmarkerHelper.close()
        eyeGestureAnalyzer.shutdown()
    }
}