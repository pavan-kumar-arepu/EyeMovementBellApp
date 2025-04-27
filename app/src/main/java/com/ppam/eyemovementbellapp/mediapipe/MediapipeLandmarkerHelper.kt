package com.ppam.eyemovementbellapp.mediapipe



import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.io.ByteArrayOutputStream
import android.graphics.Rect
import android.os.Looper
import com.google.android.datatransport.runtime.ExecutionModule_ExecutorFactory.executor

class MediapipeLandmarkerHelper(
    val context: Context,
    val runningMode: RunningMode = RunningMode.LIVE_STREAM,   // 👈 FIX: LIVE_STREAM mode
    val minFaceDetectionConfidence: Float = 0.5f,
    val minFaceTrackingConfidence: Float = 0.5f,
    val minFacePresenceConfidence: Float = 0.5f,
    val resultListener: (FaceLandmarkerResult) -> Unit
) {
    private var faceLandmarker: FaceLandmarker? = null
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    private val internalResultListener: (FaceLandmarkerResult?, MPImage?) -> Unit = { result, _ ->
        result?.let { resultListener(it) }
    }

    init {
        backgroundExecutor.execute {
            try {
                val optionsBuilder = FaceLandmarkerOptions.builder()
                    .setBaseOptions(
                        BaseOptions.builder()
                            .setModelAssetPath("face_landmarker.task")
                            .build()
                    )
                    .setRunningMode(runningMode)
                    .setNumFaces(1)
                    .setMinFacePresenceConfidence(minFacePresenceConfidence)
                    .setOutputFaceBlendshapes(true)
                    .setOutputFacialTransformationMatrixes(true)
                    .setMinTrackingConfidence(minFaceTrackingConfidence)
                    .setMinFaceDetectionConfidence(minFaceDetectionConfidence)

                if (runningMode == RunningMode.LIVE_STREAM) {
                    optionsBuilder.setResultListener(internalResultListener)  // 👈 Only set listener for LIVE_STREAM
                }

                val options = optionsBuilder.build()
                faceLandmarker = FaceLandmarker.createFromOptions(context, options)

            } catch (e: Exception) {
                Log.e("MPHelper", "Error initializing FaceLandmarker: ${e.localizedMessage}")
            }
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun detect(imageProxy: ImageProxy) {
        backgroundExecutor.execute {
            val image = imageProxy.image
            if (faceLandmarker != null && image != null) {
                try {
                    val mpImage = imageProxy.toMPImage()
                    when (runningMode) {
                        RunningMode.LIVE_STREAM -> {
                            faceLandmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
                        }
                        else -> {
                            faceLandmarker?.detect(mpImage)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MPHelper", "Detection error: ${e.localizedMessage}")
                } finally {
                    imageProxy.close()
                    image.close()
                }
            } else {
                imageProxy.close()
            }
        }
    }

    fun close() {
        backgroundExecutor.shutdown()
        faceLandmarker = null
    }
}

// --- Extensions.kt ---

@OptIn(ExperimentalGetImage::class)
fun ImageProxy.toMPImage(): MPImage {
    val bitmap = imageProxyToBitmap(this)
    return BitmapImageBuilder(bitmap).build()
}

fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
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

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
    val jpegBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
}