package com.jsf.app.pravin.domain

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.sqrt

// NOTE: Place pose_landmarker_full.task in app/src/main/assets/ before running.
// Download from: https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task
class PoseAnalyzer(private val context: Context) {

    private var poseLandmarker: PoseLandmarker? = null

    val isModelLoaded: Boolean get() = poseLandmarker != null

    fun initialize(onResult: (PoseResult) -> Unit) {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_full.task")
                .build()
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result: PoseLandmarkerResult, _: MPImage ->
                    onResult(result.toPoseResult())
                }
                .setErrorListener { error: RuntimeException -> error.printStackTrace() }
                .build()
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: Throwable) {
            e.printStackTrace()
            poseLandmarker = null
        }
    }

    fun analyze(bitmap: Bitmap, frameTime: Long) {
        poseLandmarker?.let { landmarker ->
            try {
                val mpImage = BitmapImageBuilder(bitmap).build()
                landmarker.detectAsync(mpImage, frameTime)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }
}

fun PoseLandmarkerResult.toPoseResult(): PoseResult {
    if (landmarks().isEmpty()) return PoseResult.EMPTY
    val lms = landmarks()[0].map {
        Landmark(it.x(), it.y(), it.z(), it.visibility().orElse(0f))
    }
    val bodyVisible = lms.size >= 29 && lms.subList(11, 29).all { it.visibility > 0.3f }
    return PoseResult(lms, bodyVisible)
}

fun angleBetween(a: Landmark, b: Landmark, c: Landmark): Float {
    val v1x = a.x - b.x; val v1y = a.y - b.y
    val v2x = c.x - b.x; val v2y = c.y - b.y
    val dot = v1x * v2x + v1y * v2y
    val mag1 = sqrt(v1x * v1x + v1y * v1y)
    val mag2 = sqrt(v2x * v2x + v2y * v2y)
    if (mag1 == 0f || mag2 == 0f) return 0f
    return Math.toDegrees(Math.acos((dot / (mag1 * mag2)).toDouble().coerceIn(-1.0, 1.0))).toFloat()
}
