package com.ppam.eyemovementbellapp.mediapipe



import android.content.Context
import android.graphics.Bitmap
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


class MediapipeLandmarkerHelper(
    val context: Context,
    val runningMode: RunningMode = RunningMode.LIVE_STREAM,
    val minFaceDetectionConfidence: Float = DEFAULT_MIN_FACE_DETECTION_CONFIDENCE,
    val minFaceTrackingConfidence: Float = DEFAULT_MIN_FACE_TRACKING_CONFIDENCE,
    val minFacePresenceConfidence: Float = DEFAULT_MIN_FACE_PRESENCE_CONFIDENCE,
    val resultListener: (FaceLandmarkerResult) -> Unit
) {
    private var faceLandmarker: FaceLandmarker? = null
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val internalResultListener: (FaceLandmarkerResult?, MPImage?) -> Unit =
        { result, _ ->
            result?.let {
                resultListener(it)
            }
        }

    init {
        backgroundExecutor.execute {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET_PATH)
                .setDelegate(Delegate.GPU) // Or Delegate.CPU
                .build()

            val optionsBuilder = FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(runningMode)
                .setNumFaces(1)
                .setMinFacePresenceConfidence(minFacePresenceConfidence)
                .setMinTrackingConfidence(minFaceTrackingConfidence)
                .setMinFaceDetectionConfidence(minFaceDetectionConfidence)
                .setResultListener(internalResultListener)
                .build()

            try {
                faceLandmarker = FaceLandmarker.createFromOptions(context, optionsBuilder)
                Log.e("APK", "Face Landmarker successfully to initialized!")
            } catch (e: IllegalStateException) {
                Log.e("APK", "Face Landmarker failed to initialize: ${e.localizedMessage}")
            }
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    @OptIn(ExperimentalGetImage::class)
     fun detect(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()

        backgroundExecutor.execute {
            val image = imageProxy.image
            if (faceLandmarker != null && image != null) {
                try {
//                    val mpImage = imageProxy.toMPImage(frameTime) // Use the extension function
                    val mpImage = imageProxy.toMPImage()

                    faceLandmarker?.detectAsync(mpImage, frameTime) // Pass timestamp to detectAsync
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Task is not initialized with the video mode.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during landmark detection: ${e.localizedMessage}")
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
        try {
            backgroundExecutor.awaitTermination(
                DEFAULT_TIME_OUT_FOR_CLOSE_IN_SECONDS,
                TimeUnit.SECONDS
            )
        } catch (e: InterruptedException) {
            Log.e(TAG, "Background executor failed to terminate in time.")
        }
        faceLandmarker = null
    }

    companion object {
        private const val TAG = "MPHelper"
        private const val MODEL_ASSET_PATH = "face_landmarker.task"
        private const val DEFAULT_MIN_FACE_DETECTION_CONFIDENCE = 0.5f
        private const val DEFAULT_MIN_FACE_TRACKING_CONFIDENCE = 0.5f
        private const val DEFAULT_MIN_FACE_PRESENCE_CONFIDENCE = 0.5f
        private const val DEFAULT_TIME_OUT_FOR_CLOSE_IN_SECONDS = 1L
    }
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

    val yuvImage = android.graphics.YuvImage(
        nv21,
        android.graphics.ImageFormat.NV21,
        imageProxy.width,
        imageProxy.height,
        null
    )

    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
    val jpegBytes = out.toByteArray()
    return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
}


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
private fun ImageProxy.toMPImage(): MPImage {
    val bitmap = imageProxyToBitmap(this)
    return BitmapImageBuilder(bitmap).build()
}