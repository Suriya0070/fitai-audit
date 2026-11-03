package com.jsf.app.pravin.domain

import android.graphics.Bitmap

class EnvironmentAnalyzer {

    fun analyze(bitmap: Bitmap, pose: PoseResult): EnvironmentQuality {
        val brightness = analyzeBrightness(bitmap)
        val bodyVisibility = analyzeBodyVisibility(pose)
        val cameraAngle = analyzeCameraAngle(pose)
        val sharpness = analyzeSharpness(bitmap)

        val overall = (brightness * 0.3f + bodyVisibility * 0.4f + cameraAngle * 0.2f + sharpness * 0.1f)

        val feedback = buildFeedback(brightness, bodyVisibility, cameraAngle, sharpness)

        return EnvironmentQuality(
            brightness = brightness,
            sharpness = sharpness,
            bodyVisibility = bodyVisibility,
            cameraAngle = cameraAngle,
            overall = overall,
            feedback = feedback
        )
    }

    private fun analyzeBrightness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val step = maxOf(1, (width * height) / 10000)
        var totalBrightness = 0L
        var count = 0
        for (i in 0 until width * height step step) {
            val pixel = bitmap.getPixel(i % width, i / width)
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)
            totalBrightness += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
            count++
        }
        val avg = if (count > 0) totalBrightness.toFloat() / count else 128f
        return when {
            avg < 40f -> avg / 40f * 0.3f
            avg < 80f -> 0.3f + (avg - 40f) / 40f * 0.7f
            avg < 180f -> 1f
            avg < 220f -> 1f - (avg - 180f) / 40f * 0.5f
            else -> 0.3f
        }
    }

    private fun analyzeSharpness(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        val cx = w / 2; val cy = h / 2
        var laplacian = 0.0
        val range = minOf(50, w / 4, h / 4)
        for (x in cx - range until cx + range step 4) {
            for (y in cy - range until cy + range step 4) {
                if (x < 1 || x >= w - 1 || y < 1 || y >= h - 1) continue
                val center = bitmap.getPixel(x, y).gray()
                val left = bitmap.getPixel(x - 1, y).gray()
                val right = bitmap.getPixel(x + 1, y).gray()
                val top = bitmap.getPixel(x, y - 1).gray()
                val bottom = bitmap.getPixel(x, y + 1).gray()
                val lap = (4 * center - left - right - top - bottom).toDouble()
                laplacian += lap * lap
            }
        }
        return (laplacian / 10000.0).coerceIn(0.0, 1.0).toFloat()
    }

    private fun Int.gray(): Int {
        val r = (this shr 16 and 0xFF)
        val g = (this shr 8 and 0xFF)
        val b = (this and 0xFF)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    private fun analyzeBodyVisibility(pose: PoseResult): Float {
        if (pose.landmarks.isEmpty()) return 0f
        val keyIndices = listOf(0, 11, 12, 23, 24, 25, 26, 27, 28)
        val visible = keyIndices.count { idx ->
            pose.landmarks.getOrNull(idx)?.visibility?.let { it > 0.5f } ?: false
        }
        return visible.toFloat() / keyIndices.size
    }

    private fun analyzeCameraAngle(pose: PoseResult): Float {
        val lShoulder = pose.leftShoulder() ?: return 0.5f
        val rShoulder = pose.rightShoulder() ?: return 0.5f
        val shoulderAngle = kotlin.math.atan2(
            (rShoulder.y - lShoulder.y).toDouble(),
            (rShoulder.x - lShoulder.x).toDouble()
        )
        val deviation = kotlin.math.abs(shoulderAngle) * 180.0 / Math.PI
        return (1f - (deviation / 30f).toFloat()).coerceIn(0f, 1f)
    }

    private fun buildFeedback(
        brightness: Float,
        bodyVisibility: Float,
        cameraAngle: Float,
        sharpness: Float
    ): String {
        return when {
            brightness < 0.4f -> "Too dark - please improve lighting"
            brightness > 0.9f && brightness < 1f -> "Slightly overexposed - reduce brightness"
            bodyVisibility < 0.5f -> "Full body not visible - step back from camera"
            bodyVisibility < 0.8f -> "Ensure your full body is in frame"
            cameraAngle < 0.5f -> "Adjust camera angle - point it straight ahead"
            sharpness < 0.2f -> "Image is blurry - ensure camera is stable"
            else -> "Environment looks great! Ready to workout."
        }
    }
}
