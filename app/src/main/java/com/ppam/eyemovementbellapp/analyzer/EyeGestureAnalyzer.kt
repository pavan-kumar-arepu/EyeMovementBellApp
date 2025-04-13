package com.ppam.eyemovementbellapp.analyzer


import android.content.Context
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.ppam.eyemovementbellapp.gesture.EyeDirection
import com.ppam.eyemovementbellapp.gesture.EyeDirectionDetector
import com.ppam.eyemovementbellapp.mediapipe.MediapipeLandmarkerHelper
import com.ppam.eyemovementbellapp.sound.SoundPlayer

import java.nio.ByteBuffer
/*
class EyeGestureAnalyzer(
    private val context: Context
) : ImageAnalysis.Analyzer {

    private val TAG = "EyeGestureAnalyzer"
    private val mpHelper = MediapipeLandmarkerHelper(context)


    private var lastDirection: EyeDirection = EyeDirection.NONE
    private var directionCount = 0


    fun analyze(image: ImageProxy) {
        try {
            val result: FaceLandmarkerResult? = mpHelper.detectFaceLandmarks(image)

            result?.faceLandmarks()?.firstOrNull()?.let { landmarkList ->
                val direction = EyeDirectionDetector.detectDirection(landmarkList)

                Log.d(TAG, "Detected direction: $direction")

                if (direction != EyeDirection.NONE && direction == lastDirection) {
                    directionCount++
                } else {
                    directionCount = 1
                }

                lastDirection = direction

                if (directionCount == 2) {
                    when (direction) {
                        EyeDirection.RIGHT -> {
                            SoundPlayer.playBellSound(context)
                            Log.d(TAG, "Right eye movement detected twice – playing sound")
                        }
                        EyeDirection.LEFT -> {
                            // Placeholder for LEFT action
                            Log.d(TAG, "Left eye movement detected twice – placeholder action")
                        }
                        EyeDirection.UP -> {
                            // Placeholder for UP action
                            Log.d(TAG, "Up eye movement detected twice – placeholder action")
                        }
                        EyeDirection.DOWN -> {
                            // Placeholder for DOWN action
                            Log.d(TAG, "Down eye movement detected twice – placeholder action")
                        }
                        EyeDirection.NONE -> {
                            // Nothing to do for NONE
                        }
                    }
                    directionCount = 0 // reset after action
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image", e)
        } finally {
            image.close() // Ensure image is closed after processing
        }
    }

    /*
    override fun analyze(image: ImageProxy) {
        // Convert ImageProxy to ByteBuffer and process with MediaPipe
        val result: FaceLandmarkerResult? = mpHelper.detectFaceLandmarks(image)

        result?.faceLandmarks()?.firstOrNull()?.let { landmarkList ->
            val direction = EyeDirectionDetector.detectDirection(landmarkList)

            Log.d(TAG, "Detected direction: $direction")

            if (direction != EyeDirection.NONE && direction == lastDirection) {
                directionCount++
            } else {
                directionCount = 1
            }

            lastDirection = direction

            if (directionCount == 2) {
                when (direction) {
                    EyeDirection.RIGHT -> {
                        SoundPlayer.playBellSound(context)
                        Log.d(TAG, "Right eye movement detected twice – playing sound")
                    }
                    EyeDirection.LEFT -> {
                        // TODO: Placeholder for LEFT direction
                        Log.d(TAG, "Left eye movement detected twice – placeholder action")
                    }
                    EyeDirection.UP -> {
                        // TODO: Placeholder for UP direction
                        Log.d(TAG, "Up eye movement detected twice – placeholder action")
                    }
                    EyeDirection.DOWN -> {
                        // TODO: Placeholder for DOWN direction
                        Log.d(TAG, "Down eye movement detected twice – placeholder action")
                    }
                    EyeDirection.NONE -> {
                        // Optional: Handle NONE direction case if needed
                        Log.d(TAG, "No eye movement detected")
                    }
                }

                directionCount = 0 // Reset after action
            }
        }

        image.close()  // Always close ImageProxy after processing
    }
*/
    // Convert YUV420 to RGB data
    private fun convertYUV420888ToRGB(imageProxy: ImageProxy): ByteArray {
        val planes = imageProxy.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uvSize = uBuffer.remaining()

        val yData = ByteArray(ySize)
        val uData = ByteArray(uvSize)
        val vData = ByteArray(uvSize)

        yBuffer.get(yData)
        uBuffer.get(uData)
        vBuffer.get(vData)

        val width = imageProxy.width
        val height = imageProxy.height

        // Convert YUV to RGB
        return yuv420ToRgb(yData, uData, vData, width, height)
    }

    // Convert YUV to RGB bytes
    private fun yuv420ToRgb(y: ByteArray, u: ByteArray, v: ByteArray, width: Int, height: Int): ByteArray {
        val rgb = ByteArray(width * height * 3)

        for (i in 0 until height) {
            for (j in 0 until width) {
                val yIndex = i * width + j
                val uIndex = (i / 2) * (width / 2) + (j / 2)
                val vIndex = uIndex

                val Y = y[yIndex].toInt() and 0xFF
                val U = u[uIndex].toInt() and 0xFF
                val V = v[vIndex].toInt() and 0xFF

                val r = (Y + (1.402 * (V - 128))).toInt().coerceIn(0, 255)
                val g = (Y - (0.344136 * (U - 128)) - (0.714136 * (V - 128))).toInt().coerceIn(0, 255)
                val b = (Y + (1.772 * (U - 128))).toInt().coerceIn(0, 255)

                val rgbIndex = (i * width + j) * 3
                rgb[rgbIndex] = r.toByte()
                rgb[rgbIndex + 1] = g.toByte()
                rgb[rgbIndex + 2] = b.toByte()
            }
        }

        return rgb
    }
}


 */


class EyeGestureAnalyzer(
    private val context: Context
) {

    private val TAG = "EyeGestureAnalyzer"
    private val mpHelper = MediapipeLandmarkerHelper(context)

    private var lastDirection: EyeDirection = EyeDirection.NONE
    private var directionCount = 0

    fun analyze(image: ImageProxy) {
        try {
            val result: FaceLandmarkerResult? = mpHelper.detectFaceLandmarks(image)

            result?.faceLandmarks()?.firstOrNull()?.let { landmarkList ->
                val direction = EyeDirectionDetector.detectDirection(landmarkList)

                Log.d(TAG, "Detected direction: $direction")

                if (direction != EyeDirection.NONE && direction == lastDirection) {
                    directionCount++
                } else {
                    directionCount = 1
                }

                lastDirection = direction

                if (directionCount == 2) {
                    when (direction) {
                        EyeDirection.RIGHT -> {
                            SoundPlayer.playBellSound(context)
                            Log.d(TAG, "Right eye movement detected twice – playing sound")
                        }
                        EyeDirection.LEFT -> {
                            // Placeholder for LEFT action
                            Log.d(TAG, "Left eye movement detected twice – placeholder action")
                        }
                        EyeDirection.UP -> {
                            // Placeholder for UP action
                            Log.d(TAG, "Up eye movement detected twice – placeholder action")
                        }
                        EyeDirection.DOWN -> {
                            // Placeholder for DOWN action
                            Log.d(TAG, "Down eye movement detected twice – placeholder action")
                        }
                        EyeDirection.NONE -> {
                            // Nothing to do for NONE
                        }
                    }
                    directionCount = 0 // reset after action
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image", e)
        } finally {
            image.close() // Ensure image is closed after processing
        }
    }
}
