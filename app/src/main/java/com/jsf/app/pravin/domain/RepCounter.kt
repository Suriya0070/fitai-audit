package com.jsf.app.pravin.domain

import kotlin.math.hypot

class RepCounter {
    private var repCount = 0
    private var inDownPhase = false
    private val smoothedAngles = ArrayDeque<Float>(5)
    private var lastRepTime = 0L

    fun update(pose: PoseResult, exerciseName: String): Boolean {
        val exercise = ExerciseLibrary.getByName(exerciseName) ?: return false
        if (exercise.isTimeBased || exercise.repConfig == null) return false

        val config = exercise.repConfig

        // ── 1. Distance guard (e.g. wrist spacing for push-up variants) ─────────
        val distGuard = config.distanceGuard
        if (distGuard != null) {
            val j1 = pose.getLandmark(distGuard.joint1)
            val j2 = pose.getLandmark(distGuard.joint2)
            if (j1 == null || j2 == null ||
                j1.visibility < 0.4f || j2.visibility < 0.4f) return false
            val dist = hypot(j1.x - j2.x, j1.y - j2.y)
            if (dist < distGuard.minDist || dist > distGuard.maxDist) return false
        }

        // ── 2. Angle calculation ─────────────────────────────────────────────────
        // symmetric=true  → average both sides (push-ups, squats — both sides move together)
        // symmetric=false → use min angle (lunges, high knees — the active/leading side matters)
        val leftAngle = calculateAngle(pose, config.primaryJoints)
        val rightAngle = config.mirrorJoints?.let { calculateAngle(pose, it) }

        val angle = when {
            leftAngle != null && rightAngle != null ->
                if (config.symmetric) (leftAngle + rightAngle) / 2f
                else minOf(leftAngle, rightAngle)
            leftAngle != null -> leftAngle
            rightAngle != null -> rightAngle
            else -> return false  // no visible side — skip frame
        }

        // ── 3. Smooth over last 5 frames to reduce jitter ────────────────────────
        smoothedAngles.addLast(angle)
        if (smoothedAngles.size > 5) smoothedAngles.removeFirst()
        val smoothed = smoothedAngles.average().toFloat()

        // ── 4. State machine: detect down→up cycle ────────────────────────────────
        val repCounted = if (config.invertPhase) {
            // Curls / high knees: "down" = high angle (extended), "up" = low angle (contracted)
            if (!inDownPhase && smoothed > config.downThreshold) {
                inDownPhase = true; false
            } else if (inDownPhase && smoothed < config.upThreshold) {
                inDownPhase = false
                commitRep(config.minRepIntervalMs)
            } else false
        } else {
            // Squats / push-ups: "down" = low angle (bent), "up" = high angle (extended)
            if (!inDownPhase && smoothed < config.downThreshold) {
                inDownPhase = true; false
            } else if (inDownPhase && smoothed > config.upThreshold) {
                inDownPhase = false
                commitRep(config.minRepIntervalMs)
            } else false
        }

        return repCounted
    }

    /** Commits the rep only if enough time has passed since the last one (debounce). */
    private fun commitRep(minIntervalMs: Long): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRepTime < minIntervalMs) return false
        lastRepTime = now
        repCount++
        return true
    }

    /**
     * Returns the angle at joint [b] only when all 3 landmarks are sufficiently visible.
     * Low-visibility joints produce unreliable angles and cause phantom reps.
     */
    private fun calculateAngle(pose: PoseResult, joints: JointTriplet): Float? {
        val a = pose.getLandmark(joints.a) ?: return null
        val b = pose.getLandmark(joints.b) ?: return null
        val c = pose.getLandmark(joints.c) ?: return null
        if (a.visibility < 0.5f || b.visibility < 0.5f || c.visibility < 0.5f) return null
        return angleBetween(a, b, c)
    }

    fun getCount() = repCount
    fun reset() {
        repCount = 0
        inDownPhase = false
        smoothedAngles.clear()
        lastRepTime = 0L
    }
}
