package com.ppam.eyemovementbellapp.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageProxy
//import com.google.mediapipe.tasks.vision.*
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.io.ByteArrayOutputStream
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import android.graphics.Rect
import com.google.mediapipe.framework.image.MPImageFactory


class MediapipeLandmarkerHelper(private val context: Context) {

    private var landmarker: FaceLandmarker? = null

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM) // Use LIVE_STREAM for ImageProxy input
                .setNumFaces(1)
                .build()

            landmarker = FaceLandmarker.createFromOptions(context, options)

        } catch (e: Exception) {
            Log.e("MPHelper", "Failed to initialize FaceLandmarker: ${e.message}")
        }
    }

    fun detectFaceLandmarks(imageProxy: ImageProxy): FaceLandmarkerResult? {
        return try {
            // Convert ImageProxy to MPImage using MediaPipe's factory
            val mpImage = MPImageFactory.createFromImageProxy(imageProxy)
            landmarker?.detectForVideo(mpImage, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e("MPHelper", "Error during landmark detection: ${e.message}")
            null
        }
    }

    fun close() {
        landmarker?.close()
    }
}