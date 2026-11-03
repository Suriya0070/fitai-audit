package com.jsf.app.pravin.domain

import kotlin.math.abs
import kotlin.math.sqrt

data class FormError(
    val message: String,
    val severity: Severity = Severity.MINOR
) {
    enum class Severity { MINOR, MODERATE, MAJOR }
}

/**
 * Base class for per-exercise form analysis. Each subclass inspects joint positions
 * from a PoseResult and returns a list of named FormErrors with severity.
 *
 * Errors are designed to surface actionable coaching cues — the same language a
 * personal trainer would use — rather than raw angle numbers.
 */
abstract class ExerciseFormAnalyzer {
    abstract fun analyze(pose: PoseResult): List<FormError>

    protected fun angle(a: Landmark, b: Landmark, c: Landmark): Float {
        val abx = a.x - b.x; val aby = a.y - b.y
        val cbx = c.x - b.x; val cby = c.y - b.y
        val dot = abx * cbx + aby * cby
        val magAB = sqrt(abx * abx + aby * aby)
        val magCB = sqrt(cbx * cbx + cby * cby)
        if (magAB == 0f || magCB == 0f) return 0f
        return Math.toDegrees(Math.acos((dot / (magAB * magCB)).toDouble().coerceIn(-1.0, 1.0))).toFloat()
    }

    protected fun dist(a: Landmark, b: Landmark): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    companion object {
        fun forExercise(exerciseName: String): ExerciseFormAnalyzer = when {
            exerciseName.contains("Squat", ignoreCase = true) ||
            exerciseName.contains("Wall Sit", ignoreCase = true) -> SquatFormAnalyzer()
            exerciseName.contains("Push", ignoreCase = true) ||
            exerciseName.contains("Dip", ignoreCase = true) -> PushUpFormAnalyzer()
            exerciseName.contains("Bicep", ignoreCase = true) ||
            exerciseName.contains("Curl", ignoreCase = true) -> BicepCurlFormAnalyzer()
            exerciseName.contains("Press", ignoreCase = true) ||
            exerciseName.contains("Overhead", ignoreCase = true) -> ShoulderPressFormAnalyzer()
            exerciseName.contains("Lunge", ignoreCase = true) -> LungeFormAnalyzer()
            else -> NoOpFormAnalyzer()
        }
    }
}

// ── Squat ─────────────────────────────────────────────────────────────────────
// Checks: depth, knee alignment over toes, forward lean, hip symmetry
class SquatFormAnalyzer : ExerciseFormAnalyzer() {
    override fun analyze(pose: PoseResult): List<FormError> {
        val errors = mutableListOf<FormError>()
        val lHip = pose.leftHip() ?: return errors
        val rHip = pose.rightHip() ?: return errors
        val lKnee = pose.leftKnee() ?: return errors
        val rKnee = pose.rightKnee() ?: return errors
        val lAnkle = pose.leftAnkle() ?: return errors
        val rAnkle = pose.rightAnkle() ?: return errors
        val lShoulder = pose.leftShoulder() ?: return errors
        val rShoulder = pose.rightShoulder() ?: return errors

        val leftKneeAngle = angle(lHip, lKnee, lAnkle)
        val rightKneeAngle = angle(rHip, rKnee, rAnkle)
        val avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2f

        // Knee valgus: knee x should be roughly between hip x and ankle x (not inside ankle)
        val leftKneeCave = lKnee.x > lAnkle.x + 0.05f   // left knee caving in (mirrored x-axis)
        val rightKneeCave = rKnee.x < rAnkle.x - 0.05f
        if (leftKneeCave || rightKneeCave) {
            errors += FormError("Knees caving in — push knees out over toes", FormError.Severity.MAJOR)
        }

        // Depth check: only flag during the descent (knees bent past 100°)
        if (avgKneeAngle in 100f..145f) {
            errors += FormError("Go deeper — aim for thighs parallel to floor", FormError.Severity.MODERATE)
        }

        // Forward lean: torso angle (hip–shoulder relative to vertical)
        val torsoLean = abs(
            angle(lShoulder, lHip, Landmark(lHip.x, lHip.y + 1f, 0f, 1f))
        )
        if (torsoLean > 50f && avgKneeAngle < 140f) {
            errors += FormError("Too much forward lean — chest up, weight in heels", FormError.Severity.MODERATE)
        }

        // Hip asymmetry
        if (abs(lHip.y - rHip.y) > 0.04f) {
            errors += FormError("Hips uneven — keep weight distributed equally", FormError.Severity.MINOR)
        }

        return errors
    }
}

// ── Push-up ───────────────────────────────────────────────────────────────────
// Checks: hip sag, hip pike, elbow flare, insufficient depth
class PushUpFormAnalyzer : ExerciseFormAnalyzer() {
    override fun analyze(pose: PoseResult): List<FormError> {
        val errors = mutableListOf<FormError>()
        val lShoulder = pose.leftShoulder() ?: return errors
        val rShoulder = pose.rightShoulder() ?: return errors
        val lElbow = pose.leftElbow() ?: return errors
        val rElbow = pose.rightElbow() ?: return errors
        val lWrist = pose.leftWrist() ?: return errors
        val rWrist = pose.rightWrist() ?: return errors
        val lHip = pose.leftHip() ?: return errors
        val rHip = pose.rightHip() ?: return errors

        val hipY = (lHip.y + rHip.y) / 2f
        val shoulderY = (lShoulder.y + rShoulder.y) / 2f
        val hipDrop = hipY - shoulderY   // positive = hips lower than shoulders (sag)

        if (hipDrop > 0.06f) {
            errors += FormError("Hips sagging — engage your core and glutes", FormError.Severity.MAJOR)
        }
        if (hipDrop < -0.08f) {
            errors += FormError("Hips too high — lower them into a straight plank", FormError.Severity.MODERATE)
        }

        // Elbow flare: elbow should track roughly over wrist, not flare beyond shoulder width
        val leftElbowFlare = lElbow.x < lShoulder.x - 0.06f
        val rightElbowFlare = rElbow.x > rShoulder.x + 0.06f
        if (leftElbowFlare || rightElbowFlare) {
            errors += FormError("Elbows flaring out — tuck them closer to your sides", FormError.Severity.MODERATE)
        }

        // Elbow angle — check depth (only flag when clearly not low enough)
        val leftElbowAngle = angle(lShoulder, lElbow, lWrist)
        val rightElbowAngle = angle(rShoulder, rElbow, rWrist)
        val avgElbow = (leftElbowAngle + rightElbowAngle) / 2f
        if (avgElbow > 130f) {
            errors += FormError("Not going low enough — lower chest closer to floor", FormError.Severity.MODERATE)
        }

        // Shoulder asymmetry
        if (abs(lShoulder.y - rShoulder.y) > 0.04f) {
            errors += FormError("Shoulders uneven — keep the body square", FormError.Severity.MINOR)
        }

        return errors
    }
}

// ── Bicep Curl ────────────────────────────────────────────────────────────────
// Checks: elbow drift, back swing, incomplete range
class BicepCurlFormAnalyzer : ExerciseFormAnalyzer() {
    override fun analyze(pose: PoseResult): List<FormError> {
        val errors = mutableListOf<FormError>()
        val lShoulder = pose.leftShoulder() ?: return errors
        val rShoulder = pose.rightShoulder() ?: return errors
        val lElbow = pose.leftElbow() ?: return errors
        val rElbow = pose.rightElbow() ?: return errors
        val lWrist = pose.leftWrist() ?: return errors
        val rWrist = pose.rightWrist() ?: return errors
        val lHip = pose.leftHip() ?: return errors
        val rHip = pose.rightHip() ?: return errors

        // Elbow drift: elbow should stay at the side — flag if it moves significantly forward
        val leftElbowDrift = lShoulder.x - lElbow.x  // how far elbow is in front of shoulder
        val rightElbowDrift = rElbow.x - rShoulder.x
        if (leftElbowDrift > 0.07f || rightElbowDrift > 0.07f) {
            errors += FormError("Elbows drifting forward — pin them to your sides", FormError.Severity.MODERATE)
        }

        // Back swing: hip movement indicates using momentum
        val hipMidX = (lHip.x + rHip.x) / 2f
        val shoulderMidX = (lShoulder.x + rShoulder.x) / 2f
        if (abs(hipMidX - shoulderMidX) > 0.06f) {
            errors += FormError("Swinging the back — use slower, controlled reps", FormError.Severity.MODERATE)
        }

        // Range of motion check
        val leftAngle = angle(lShoulder, lElbow, lWrist)
        val rightAngle = angle(rShoulder, rElbow, rWrist)
        val avgAngle = (leftAngle + rightAngle) / 2f
        if (avgAngle in 90f..130f) {
            errors += FormError("Partial range — curl all the way up and lower fully", FormError.Severity.MINOR)
        }

        // Symmetry
        if (abs(leftAngle - rightAngle) > 20f) {
            errors += FormError("Arms not in sync — keep both arms moving together", FormError.Severity.MINOR)
        }

        return errors
    }
}

// ── Shoulder Press ────────────────────────────────────────────────────────────
// Checks: lower-back arch, full extension, elbow width, asymmetry
class ShoulderPressFormAnalyzer : ExerciseFormAnalyzer() {
    override fun analyze(pose: PoseResult): List<FormError> {
        val errors = mutableListOf<FormError>()
        val lShoulder = pose.leftShoulder() ?: return errors
        val rShoulder = pose.rightShoulder() ?: return errors
        val lElbow = pose.leftElbow() ?: return errors
        val rElbow = pose.rightElbow() ?: return errors
        val lWrist = pose.leftWrist() ?: return errors
        val rWrist = pose.rightWrist() ?: return errors
        val lHip = pose.leftHip() ?: return errors
        val rHip = pose.rightHip() ?: return errors

        // Lower-back arch: hips should stay roughly level with shoulders during the press
        val hipY = (lHip.y + rHip.y) / 2f
        val shoulderY = (lShoulder.y + rShoulder.y) / 2f
        if (hipY < shoulderY - 0.15f) {
            errors += FormError("Arching lower back — brace your core, press vertically", FormError.Severity.MAJOR)
        }

        // Full extension at top: wrist should be clearly above shoulder
        val leftExtension = angle(lWrist, lElbow, lShoulder)
        val rightExtension = angle(rWrist, rElbow, rShoulder)
        val avgExtension = (leftExtension + rightExtension) / 2f
        if (avgExtension < 140f) {
            errors += FormError("Not fully extended — press arms straight overhead", FormError.Severity.MODERATE)
        }

        // Elbows too wide: elbow x should not be much wider than shoulder x
        val shoulderWidth = abs(lShoulder.x - rShoulder.x)
        val elbowWidth = abs(lElbow.x - rElbow.x)
        if (elbowWidth > shoulderWidth * 1.5f) {
            errors += FormError("Elbows too wide — bring them forward and in", FormError.Severity.MINOR)
        }

        // Asymmetry
        if (abs(leftExtension - rightExtension) > 25f) {
            errors += FormError("One arm leading — press with both arms equally", FormError.Severity.MINOR)
        }

        return errors
    }
}

// ── Lunge ─────────────────────────────────────────────────────────────────────
// Checks: front knee past toes, torso lean, back knee close to floor, hip drop
class LungeFormAnalyzer : ExerciseFormAnalyzer() {
    override fun analyze(pose: PoseResult): List<FormError> {
        val errors = mutableListOf<FormError>()
        val lHip = pose.leftHip() ?: return errors
        val rHip = pose.rightHip() ?: return errors
        val lKnee = pose.leftKnee() ?: return errors
        val rKnee = pose.rightKnee() ?: return errors
        val lAnkle = pose.leftAnkle() ?: return errors
        val rAnkle = pose.rightAnkle() ?: return errors
        val lShoulder = pose.leftShoulder() ?: return errors
        val rShoulder = pose.rightShoulder() ?: return errors

        // Front knee over toes: knee x should not go past ankle x by more than a small margin
        // (In normalized coords with front leg, forward means positive x shift on the leading side)
        val leftKneePast = (lKnee.x - lAnkle.x)
        val rightKneePast = (rAnkle.x - rKnee.x)
        if (leftKneePast > 0.08f || rightKneePast > 0.08f) {
            errors += FormError("Front knee past toes — step further forward", FormError.Severity.MAJOR)
        }

        // Torso upright: check shoulder alignment over hips
        val shoulderMidX = (lShoulder.x + rShoulder.x) / 2f
        val hipMidX = (lHip.x + rHip.x) / 2f
        if (abs(shoulderMidX - hipMidX) > 0.07f) {
            errors += FormError("Torso leaning forward — keep chest up and tall", FormError.Severity.MODERATE)
        }

        // Hip drop: hips should remain level
        if (abs(lHip.y - rHip.y) > 0.05f) {
            errors += FormError("Hips dropping to one side — keep them level", FormError.Severity.MODERATE)
        }

        // Back knee depth: back knee should come close to the floor
        val backKneeY = maxOf(lKnee.y, rKnee.y)   // higher y = lower in frame
        val frontKneeY = minOf(lKnee.y, rKnee.y)
        if (backKneeY - frontKneeY < 0.05f) {
            errors += FormError("Drop lower — back knee should nearly touch the floor", FormError.Severity.MINOR)
        }

        return errors
    }
}

// Fallback for exercises without a dedicated analyzer
class NoOpFormAnalyzer : ExerciseFormAnalyzer() {
    override fun analyze(pose: PoseResult): List<FormError> = emptyList()
}
