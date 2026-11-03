package com.jsf.app.pravin.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class WorkoutScoreEngine {

    private val recentAngles = ArrayDeque<Float>(30)
    private var lastRepTime = 0L
    private val repDurations = ArrayDeque<Long>(10)

    fun calculateScore(pose: PoseResult, exerciseName: String, currentTime: Long): WorkoutScore {
        if (pose.landmarks.isEmpty()) return WorkoutScore()
        val exercise = ExerciseLibrary.getByName(exerciseName)
            ?: return scoreKneeFlexion(pose, currentTime)
        return when (exercise.scoreType) {
            ScoreType.KNEE_FLEXION    -> scoreKneeFlexion(pose, currentTime)
            ScoreType.ELBOW_PUSH      -> scoreElbowPush(pose, currentTime)
            ScoreType.ELBOW_PULL      -> scoreElbowPull(pose, currentTime)
            ScoreType.ELBOW_OVERHEAD  -> scoreElbowOverhead(pose, currentTime)
            ScoreType.HIP_EXTENSION   -> scoreHipExtension(pose, currentTime)
            ScoreType.HIP_HINGE       -> scoreHipHinge(pose, currentTime)
            ScoreType.TRUNK_FLEXION   -> scoreTrunkFlexion(pose, currentTime)
            ScoreType.LEG_RAISE       -> scoreLegRaise(pose, currentTime)
            ScoreType.STATIC_HOLD     -> scorePlank(pose, currentTime)
            ScoreType.DYNAMIC_CARDIO  -> scoreDynamicCardio(pose, currentTime)
            ScoreType.CALF_RAISE      -> scoreCalfRaise(pose, currentTime)
        }
    }

    // ── Knee Flexion (squats, lunges, jump squats, sumo squats) ───────────────
    private fun scoreKneeFlexion(pose: PoseResult, time: Long): WorkoutScore {
        val lHip = pose.leftHip() ?: return WorkoutScore()
        val rHip = pose.rightHip() ?: return WorkoutScore()
        val lKnee = pose.leftKnee() ?: return WorkoutScore()
        val rKnee = pose.rightKnee() ?: return WorkoutScore()
        val lAnkle = pose.leftAnkle() ?: return WorkoutScore()
        val rAnkle = pose.rightAnkle() ?: return WorkoutScore()

        val leftAngle = angleBetween(lHip, lKnee, lAnkle)
        val rightAngle = angleBetween(rHip, rKnee, rAnkle)
        val avg = (leftAngle + rightAngle) / 2f

        recentAngles.addLast(avg)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        val accuracy = when {
            avg < 90f  -> normalize(avg, 60f, 90f, 70f, 100f)
            avg < 130f -> 100f
            else       -> normalize(avg, 130f, 175f, 100f, 50f)
        }
        val balance    = max(0f, 100f - abs(lHip.y - rHip.y) * 500f)
        val stability  = stabilityScore()
        val symmetry   = max(0f, 100f - abs(leftAngle - rightAngle) * 3f)
        val speed      = scoreSpeed(time)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = stability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = speed.coerceIn(0f, 100f)
        )
    }

    // ── Elbow Push (push-ups, dips, pike push-ups) ────────────────────────────
    private fun scoreElbowPush(pose: PoseResult, time: Long): WorkoutScore {
        val lShoulder = pose.leftShoulder() ?: return WorkoutScore()
        val rShoulder = pose.rightShoulder() ?: return WorkoutScore()
        val lElbow    = pose.leftElbow() ?: return WorkoutScore()
        val rElbow    = pose.rightElbow() ?: return WorkoutScore()
        val lWrist    = pose.leftWrist() ?: return WorkoutScore()
        val rWrist    = pose.rightWrist() ?: return WorkoutScore()
        val lHip      = pose.leftHip() ?: return WorkoutScore()
        val rHip      = pose.rightHip() ?: return WorkoutScore()

        val leftAngle  = angleBetween(lShoulder, lElbow, lWrist)
        val rightAngle = angleBetween(rShoulder, rElbow, rWrist)
        val avg = (leftAngle + rightAngle) / 2f

        recentAngles.addLast(avg)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        val accuracy = when {
            avg < 90f  -> 100f
            avg < 130f -> normalize(avg, 90f, 130f, 100f, 70f)
            else       -> normalize(avg, 130f, 175f, 70f, 40f)
        }

        val shoulderDiff = abs(lShoulder.y - rShoulder.y)
        val balance   = max(0f, 100f - shoulderDiff * 300f)
        val stability = stabilityScore()
        val symmetry  = max(0f, 100f - abs(leftAngle - rightAngle) * 3f)

        // Body alignment: hips should be level with shoulders in plank position
        val hipMid      = (lHip.y + rHip.y) / 2f
        val shoulderMid = (lShoulder.y + rShoulder.y) / 2f
        val alignBonus  = max(0f, 100f - abs(hipMid - shoulderMid) * 200f)
        val combinedStability = (stability * 0.7f + alignBonus * 0.3f)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = combinedStability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = scoreSpeed(time).coerceIn(0f, 100f)
        )
    }

    // ── Elbow Pull (bicep curls, hammer curls) ────────────────────────────────
    private fun scoreElbowPull(pose: PoseResult, time: Long): WorkoutScore {
        val lShoulder = pose.leftShoulder() ?: return WorkoutScore()
        val rShoulder = pose.rightShoulder() ?: return WorkoutScore()
        val lElbow    = pose.leftElbow() ?: return WorkoutScore()
        val rElbow    = pose.rightElbow() ?: return WorkoutScore()
        val lWrist    = pose.leftWrist() ?: return WorkoutScore()
        val rWrist    = pose.rightWrist() ?: return WorkoutScore()

        val leftAngle  = angleBetween(lShoulder, lElbow, lWrist)
        val rightAngle = angleBetween(rShoulder, rElbow, rWrist)
        val avg = (leftAngle + rightAngle) / 2f

        recentAngles.addLast(avg)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        // Accuracy: full range — bottom < 50°, top > 155°
        val accuracy = when {
            avg < 50f  -> 100f
            avg < 90f  -> normalize(avg, 50f, 90f, 100f, 70f)
            avg < 140f -> normalize(avg, 90f, 140f, 70f, 85f)
            else       -> 100f
        }
        val shoulderDiff = abs(lShoulder.y - rShoulder.y)
        val balance   = max(0f, 100f - shoulderDiff * 300f)
        val stability = stabilityScore()
        val symmetry  = max(0f, 100f - abs(leftAngle - rightAngle) * 3f)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = stability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = scoreSpeed(time).coerceIn(0f, 100f)
        )
    }

    // ── Elbow Overhead (tricep extensions) ────────────────────────────────────
    private fun scoreElbowOverhead(pose: PoseResult, time: Long): WorkoutScore {
        val lShoulder = pose.leftShoulder() ?: return WorkoutScore()
        val rShoulder = pose.rightShoulder() ?: return WorkoutScore()
        val lElbow    = pose.leftElbow() ?: return WorkoutScore()
        val rElbow    = pose.rightElbow() ?: return WorkoutScore()
        val lWrist    = pose.leftWrist() ?: return WorkoutScore()
        val rWrist    = pose.rightWrist() ?: return WorkoutScore()

        // Overhead: angle is wrist-elbow-shoulder
        val leftAngle  = angleBetween(lWrist, lElbow, lShoulder)
        val rightAngle = angleBetween(rWrist, rElbow, rShoulder)
        val avg = (leftAngle + rightAngle) / 2f

        recentAngles.addLast(avg)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        // Full extension overhead = ~155-170°, bottom = ~60-80°
        val accuracy = when {
            avg > 150f -> 100f
            avg > 120f -> normalize(avg, 120f, 150f, 70f, 100f)
            else       -> normalize(avg, 60f, 120f, 50f, 70f)
        }
        val balance   = max(0f, 100f - abs(lShoulder.y - rShoulder.y) * 300f)
        val stability = stabilityScore()
        val symmetry  = max(0f, 100f - abs(leftAngle - rightAngle) * 3f)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = stability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = scoreSpeed(time).coerceIn(0f, 100f)
        )
    }

    // ── Hip Extension (glute bridges, donkey kicks, hip thrusts) ─────────────
    private fun scoreHipExtension(pose: PoseResult, time: Long): WorkoutScore {
        val lShoulder = pose.leftShoulder() ?: return WorkoutScore()
        val rShoulder = pose.rightShoulder() ?: return WorkoutScore()
        val lHip      = pose.leftHip() ?: return WorkoutScore()
        val rHip      = pose.rightHip() ?: return WorkoutScore()
        val lKnee     = pose.leftKnee() ?: return WorkoutScore()
        val rKnee     = pose.rightKnee() ?: return WorkoutScore()

        val leftAngle  = angleBetween(lKnee, lHip, lShoulder)
        val rightAngle = angleBetween(rKnee, rHip, rShoulder)
        val avg = (leftAngle + rightAngle) / 2f

        recentAngles.addLast(avg)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        // Ideal bridge: angle at hip ~ 160-180° at top
        val accuracy = when {
            avg > 155f -> 100f
            avg > 130f -> normalize(avg, 130f, 155f, 70f, 100f)
            else       -> normalize(avg, 80f, 130f, 40f, 70f)
        }
        val hipDiff   = abs(lHip.y - rHip.y)
        val balance   = max(0f, 100f - hipDiff * 400f)
        val stability = stabilityScore()
        val symmetry  = max(0f, 100f - abs(leftAngle - rightAngle) * 3f)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = stability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = scoreSpeed(time).coerceIn(0f, 100f)
        )
    }

    // ── Hip Hinge (good mornings, superman, RDL) ──────────────────────────────
    private fun scoreHipHinge(pose: PoseResult, time: Long): WorkoutScore {
        val lShoulder = pose.leftShoulder() ?: return WorkoutScore()
        val rShoulder = pose.rightShoulder() ?: return WorkoutScore()
        val lHip      = pose.leftHip() ?: return WorkoutScore()
        val rHip      = pose.rightHip() ?: return WorkoutScore()
        val lKnee     = pose.leftKnee() ?: return WorkoutScore()
        val rKnee     = pose.rightKnee() ?: return WorkoutScore()

        val leftAngle  = angleBetween(lShoulder, lHip, lKnee)
        val rightAngle = angleBetween(rShoulder, rHip, rKnee)
        val avg = (leftAngle + rightAngle) / 2f

        recentAngles.addLast(avg)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        // Standing: ~160-175°, hinged: ~70-90°
        val accuracy = when {
            avg < 90f  -> 100f
            avg < 120f -> normalize(avg, 90f, 120f, 100f, 70f)
            else       -> normalize(avg, 120f, 175f, 70f, 50f)
        }
        val hipDiff   = abs(lHip.y - rHip.y)
        val balance   = max(0f, 100f - hipDiff * 400f)
        val stability = stabilityScore()
        val symmetry  = max(0f, 100f - abs(leftAngle - rightAngle) * 2f)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = stability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = scoreSpeed(time).coerceIn(0f, 100f)
        )
    }

    // ── Trunk Flexion (crunches, sit-ups, bicycle crunches) ───────────────────
    private fun scoreTrunkFlexion(pose: PoseResult, time: Long): WorkoutScore {
        val lShoulder = pose.leftShoulder() ?: return WorkoutScore()
        val rShoulder = pose.rightShoulder() ?: return WorkoutScore()
        val lHip      = pose.leftHip() ?: return WorkoutScore()
        val rHip      = pose.rightHip() ?: return WorkoutScore()
        val lKnee     = pose.leftKnee() ?: return WorkoutScore()
        val rKnee     = pose.rightKnee() ?: return WorkoutScore()

        val leftAngle  = angleBetween(lShoulder, lHip, lKnee)
        val rightAngle = angleBetween(rShoulder, rHip, rKnee)
        val avg = (leftAngle + rightAngle) / 2f

        recentAngles.addLast(avg)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        // Sit-up top: ~60-80°, lying flat: ~160-175°
        val accuracy = when {
            avg < 80f  -> 100f
            avg < 110f -> normalize(avg, 80f, 110f, 100f, 75f)
            else       -> normalize(avg, 110f, 175f, 75f, 40f)
        }
        val shoulderDiff = abs(lShoulder.y - rShoulder.y)
        val balance   = max(0f, 100f - shoulderDiff * 300f)
        val stability = stabilityScore()
        val symmetry  = max(0f, 100f - abs(leftAngle - rightAngle) * 2f)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = stability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = scoreSpeed(time).coerceIn(0f, 100f)
        )
    }

    // ── Leg Raise ─────────────────────────────────────────────────────────────
    private fun scoreLegRaise(pose: PoseResult, time: Long): WorkoutScore {
        val lShoulder = pose.leftShoulder() ?: return WorkoutScore()
        val rShoulder = pose.rightShoulder() ?: return WorkoutScore()
        val lHip      = pose.leftHip() ?: return WorkoutScore()
        val rHip      = pose.rightHip() ?: return WorkoutScore()
        val lKnee     = pose.leftKnee() ?: return WorkoutScore()
        val rKnee     = pose.rightKnee() ?: return WorkoutScore()

        val leftAngle  = angleBetween(lShoulder, lHip, lKnee)
        val rightAngle = angleBetween(rShoulder, rHip, rKnee)
        val avg = (leftAngle + rightAngle) / 2f

        recentAngles.addLast(avg)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        // Legs at 90° = ~90° angle, legs lowered = ~160°+
        val accuracy = when {
            avg < 90f  -> 100f
            avg < 120f -> normalize(avg, 90f, 120f, 100f, 70f)
            else       -> normalize(avg, 120f, 165f, 70f, 40f)
        }
        val hipDiff   = abs(lHip.y - rHip.y)
        val balance   = max(0f, 100f - hipDiff * 400f)
        val stability = stabilityScore()
        val symmetry  = max(0f, 100f - abs(leftAngle - rightAngle) * 2f)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = stability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = scoreSpeed(time).coerceIn(0f, 100f)
        )
    }

    // ── Static Hold (plank, wall sit, bear crawl) ─────────────────────────────
    private fun scorePlank(pose: PoseResult, @Suppress("UNUSED_PARAMETER") time: Long): WorkoutScore {
        val lShoulder = pose.leftShoulder() ?: return WorkoutScore()
        val rShoulder = pose.rightShoulder() ?: return WorkoutScore()
        val lHip      = pose.leftHip() ?: return WorkoutScore()
        val rHip      = pose.rightHip() ?: return WorkoutScore()

        val hipHeight      = (lHip.y + rHip.y) / 2f
        val shoulderHeight = (lShoulder.y + rShoulder.y) / 2f
        val alignment      = kotlin.math.abs(hipHeight - shoulderHeight)

        recentAngles.addLast(hipHeight * 100f)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        val accuracy  = max(0f, 100f - alignment * 200f)
        val sideDiff  = abs(lShoulder.y - rShoulder.y)
        val balance   = max(0f, 100f - sideDiff * 300f)
        val stability = stabilityScore()
        val symmetry  = max(0f, 100f - sideDiff * 300f)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = stability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = 80f  // static hold — speed not relevant
        )
    }

    // ── Dynamic Cardio (burpees, high knees, jumping jacks, mountain climbers) ─
    private fun scoreDynamicCardio(pose: PoseResult, time: Long): WorkoutScore {
        val lShoulder = pose.leftShoulder() ?: return WorkoutScore()
        val rShoulder = pose.rightShoulder() ?: return WorkoutScore()
        val lHip      = pose.leftHip() ?: return WorkoutScore()
        val rHip      = pose.rightHip() ?: return WorkoutScore()
        val lKnee     = pose.leftKnee() ?: return WorkoutScore()
        val rKnee     = pose.rightKnee() ?: return WorkoutScore()

        val hipMid  = (lHip.y + rHip.y) / 2f
        recentAngles.addLast(hipMid * 100f)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        val hipDiff      = abs(lHip.y - rHip.y)
        val shoulderDiff = abs(lShoulder.y - rShoulder.y)
        val kneeDiff     = abs(lKnee.y - rKnee.y)

        val accuracy  = 80f  // hard to pin ideal angles in dynamic compound moves
        val balance   = max(0f, 100f - (hipDiff + shoulderDiff) * 250f)
        val stability = stabilityScore()
        val symmetry  = max(0f, 100f - kneeDiff * 200f)
        val speed     = scoreSpeed(time)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = stability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = speed.coerceIn(0f, 100f)
        )
    }

    // ── Calf Raise ────────────────────────────────────────────────────────────
    private fun scoreCalfRaise(pose: PoseResult, time: Long): WorkoutScore {
        val lKnee  = pose.leftKnee() ?: return WorkoutScore()
        val rKnee  = pose.rightKnee() ?: return WorkoutScore()
        val lAnkle = pose.leftAnkle() ?: return WorkoutScore()
        val rAnkle = pose.rightAnkle() ?: return WorkoutScore()
        val lHeel  = pose.leftHeel() ?: return WorkoutScore()
        val rHeel  = pose.rightHeel() ?: return WorkoutScore()

        val leftAngle  = angleBetween(lKnee, lAnkle, lHeel)
        val rightAngle = angleBetween(rKnee, rAnkle, rHeel)
        val avg = (leftAngle + rightAngle) / 2f

        recentAngles.addLast(avg)
        if (recentAngles.size > 30) recentAngles.removeFirst()

        // Raised = larger angle (heel pulled back), flat = smaller angle
        val accuracy = when {
            avg > 110f -> 100f
            avg > 90f  -> normalize(avg, 90f, 110f, 70f, 100f)
            else       -> normalize(avg, 70f, 90f, 40f, 70f)
        }
        val balance   = max(0f, 100f - abs(lAnkle.y - rAnkle.y) * 400f)
        val stability = stabilityScore()
        val symmetry  = max(0f, 100f - abs(leftAngle - rightAngle) * 3f)

        return WorkoutScore(
            accuracy  = accuracy.coerceIn(0f, 100f),
            balance   = balance.coerceIn(0f, 100f),
            stability = stability.coerceIn(0f, 100f),
            symmetry  = symmetry.coerceIn(0f, 100f),
            speed     = scoreSpeed(time).coerceIn(0f, 100f)
        )
    }

    // ── Shared utilities ──────────────────────────────────────────────────────

    private fun stabilityScore(): Float {
        if (recentAngles.size < 5) return 80f
        val mean = recentAngles.average().toFloat()
        val variance = recentAngles.map { (it - mean) * (it - mean) }.average().toFloat()
        return max(0f, 100f - variance * 0.5f)
    }

    private fun scoreSpeed(currentTime: Long): Float {
        if (lastRepTime == 0L) { lastRepTime = currentTime; return 80f }
        return when (val elapsed = currentTime - lastRepTime) {
            in 0..999    -> 40f  // too fast
            in 1000..1999 -> 70f
            in 2000..4000 -> 100f
            in 4001..6000 -> 70f
            else          -> 40f  // too slow
        }
    }

    fun onRepCompleted(time: Long) {
        repDurations.addLast(time - lastRepTime)
        if (repDurations.size > 10) repDurations.removeFirst()
        lastRepTime = time
    }

    fun reset() {
        recentAngles.clear()
        repDurations.clear()
        lastRepTime = 0L
    }

    private fun normalize(value: Float, min: Float, max: Float, outMin: Float, outMax: Float): Float {
        if (max == min) return outMin
        return outMin + (value - min) / (max - min) * (outMax - outMin)
    }
}
