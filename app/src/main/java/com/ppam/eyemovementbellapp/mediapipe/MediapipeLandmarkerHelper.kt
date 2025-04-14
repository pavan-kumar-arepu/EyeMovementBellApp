package com.ppam.eyemovementbellapp.mediapipe
import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
import java.io.ByteArrayOutputStream
class MediapipeLandmarkerHelper(
    private val context: Context,
    private val resultListener: (FaceLandmarkerResult) -> Unit // Add result listener here
) {

    private var landmarker: FaceLandmarker? = null
    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")  // Make sure this is the correct path
                .build()

            val options = FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM) // Ensure the mode is live stream
                .setNumFaces(1) // Number of faces to track (can be adjusted)
                .setResultListener { result, _ ->
                    resultListener(result)  // Pass result to listener
                }
                .build()

            Log.d("APK: MPHelper", "FaceLandmarker Before initialization")

            landmarker = FaceLandmarker.createFromOptions(context, options)
            Log.d("APK: MPHelper", "FaceLandmarker initialized successfully")
        } catch (e: Exception) {
            Log.e("APK: MPHelper", "Failed to initialize FaceLandmarker: ${e.message}")
        }
    }

    fun detectFaceLandmarks(imageProxy: ImageProxy): FaceLandmarkerResult? {
        return try {
            val bitmap = imageProxyToBitmap(imageProxy)
            val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
            landmarker?.detectForVideo(mpImage, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e("APK: MPHelper", "Error during landmark detection: ${e.message}")
            null
        } finally {
            imageProxy.close()
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun close() {
        landmarker?.close()
    }
}