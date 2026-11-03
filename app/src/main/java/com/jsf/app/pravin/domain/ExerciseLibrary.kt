package com.jsf.app.pravin.domain

object ExerciseLibrary {

    // MediaPipe BlazePose 33-landmark indices
    private const val L_SHOULDER = 11
    private const val R_SHOULDER = 12
    private const val L_ELBOW = 13
    private const val R_ELBOW = 14
    private const val L_WRIST = 15
    private const val R_WRIST = 16
    private const val L_HIP = 23
    private const val R_HIP = 24
    private const val L_KNEE = 25
    private const val R_KNEE = 26
    private const val L_ANKLE = 27
    private const val R_ANKLE = 28
    private const val L_HEEL = 29
    private const val R_HEEL = 30

    val all: List<ExerciseDefinition> = listOf(

        // ── CHEST ──────────────────────────────────────────────────────────────
        ExerciseDefinition(
            name = "Push-ups",
            emoji = "💪",
            category = ExerciseCategory.CHEST,
            primaryMuscle = "Chest",
            secondaryMuscles = listOf("Triceps", "Shoulders"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_ELBOW, L_WRIST),
                mirrorJoints = JointTriplet(R_SHOULDER, R_ELBOW, R_WRIST),
                downThreshold = 90f, upThreshold = 155f,
                // Normal push-ups: wrists roughly shoulder-width (0.12–0.40 normalized)
                distanceGuard = JointDistanceGuard(L_WRIST, R_WRIST, minDist = 0.12f, maxDist = 0.40f)
            ),
            formTips = listOf(
                "Start in high plank, hands slightly wider than shoulders",
                "Keep your body in a straight line — no sagging hips",
                "Lower your chest until it nearly touches the floor",
                "Push back up, fully extending your arms",
                "Breathe in on the way down, out on the way up"
            ),
            scoreType = ScoreType.ELBOW_PUSH,
            musclesWorked = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)
        ),
        ExerciseDefinition(
            name = "Wide Push-ups",
            emoji = "🤲",
            category = ExerciseCategory.CHEST,
            primaryMuscle = "Chest (outer)",
            secondaryMuscles = listOf("Triceps", "Shoulders"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_ELBOW, L_WRIST),
                mirrorJoints = JointTriplet(R_SHOULDER, R_ELBOW, R_WRIST),
                downThreshold = 95f, upThreshold = 155f,
                // Wide push-ups: wrists noticeably wider than shoulders (> 0.38 normalized)
                distanceGuard = JointDistanceGuard(L_WRIST, R_WRIST, minDist = 0.38f, maxDist = 1.0f)
            ),
            formTips = listOf(
                "Place hands wider than shoulder-width apart",
                "Elbows flare out slightly to target outer chest",
                "Keep your core tight and hips level",
                "Lower until chest is close to the floor",
                "Press back to start in a controlled motion"
            ),
            scoreType = ScoreType.ELBOW_PUSH,
            musclesWorked = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)
        ),
        ExerciseDefinition(
            name = "Diamond Push-ups",
            emoji = "💎",
            category = ExerciseCategory.CHEST,
            primaryMuscle = "Triceps",
            secondaryMuscles = listOf("Chest (inner)", "Shoulders"),
            difficulty = Difficulty.INTERMEDIATE,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_ELBOW, L_WRIST),
                mirrorJoints = JointTriplet(R_SHOULDER, R_ELBOW, R_WRIST),
                downThreshold = 75f, upThreshold = 150f,
                // Diamond push-ups: wrists touching/very close (< 0.14 normalized)
                distanceGuard = JointDistanceGuard(L_WRIST, R_WRIST, minDist = 0f, maxDist = 0.14f)
            ),
            formTips = listOf(
                "Form a diamond shape with your thumbs and index fingers",
                "Place hands under your chest, not your shoulders",
                "Elbows track close to your body as you lower",
                "Lower slowly — this targets triceps more than chest",
                "Fully extend at the top on each rep"
            ),
            scoreType = ScoreType.ELBOW_PUSH,
            musclesWorked = listOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST, MuscleGroup.SHOULDERS)
        ),
        ExerciseDefinition(
            name = "Incline Push-ups",
            emoji = "📐",
            category = ExerciseCategory.CHEST,
            primaryMuscle = "Lower Chest",
            secondaryMuscles = listOf("Triceps", "Shoulders"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_ELBOW, L_WRIST),
                mirrorJoints = JointTriplet(R_SHOULDER, R_ELBOW, R_WRIST),
                downThreshold = 100f, upThreshold = 160f
            ),
            formTips = listOf(
                "Place hands on a raised surface — chair, bench, or wall",
                "Body forms a diagonal straight line",
                "Easier than standard push-ups — great for beginners",
                "Lower chest toward the surface, elbows at 45°",
                "Push back to start, maintaining a straight body"
            ),
            scoreType = ScoreType.ELBOW_PUSH,
            musclesWorked = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)
        ),
        ExerciseDefinition(
            name = "Decline Push-ups",
            emoji = "⬇️",
            category = ExerciseCategory.CHEST,
            primaryMuscle = "Upper Chest",
            secondaryMuscles = listOf("Shoulders", "Triceps"),
            difficulty = Difficulty.ADVANCED,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_ELBOW, L_WRIST),
                mirrorJoints = JointTriplet(R_SHOULDER, R_ELBOW, R_WRIST),
                downThreshold = 85f, upThreshold = 155f
            ),
            formTips = listOf(
                "Place feet on a raised surface, hands on the floor",
                "More body weight on arms — harder than standard",
                "Targets upper chest and anterior deltoids",
                "Keep hips in line with shoulders and heels",
                "Control the descent for maximum tension"
            ),
            scoreType = ScoreType.ELBOW_PUSH,
            musclesWorked = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
        ),

        // ── BACK ──────────────────────────────────────────────────────────────
        ExerciseDefinition(
            name = "Superman",
            emoji = "🦸",
            category = ExerciseCategory.BACK,
            primaryMuscle = "Lower Back",
            secondaryMuscles = listOf("Glutes", "Hamstrings"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_KNEE, L_HIP, L_SHOULDER),
                mirrorJoints = JointTriplet(R_KNEE, R_HIP, R_SHOULDER),
                downThreshold = 155f, upThreshold = 175f,
                invertPhase = true
            ),
            formTips = listOf(
                "Lie face down, arms extended straight in front of you",
                "Simultaneously lift arms, chest, and legs off the floor",
                "Squeeze your glutes and lower back at the top",
                "Hold 1–2 seconds at the top before lowering",
                "Control the descent — don't just drop"
            ),
            scoreType = ScoreType.HIP_HINGE,
            musclesWorked = listOf(MuscleGroup.LOWER_BACK, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS)
        ),
        ExerciseDefinition(
            name = "Good Mornings",
            emoji = "🌅",
            category = ExerciseCategory.BACK,
            primaryMuscle = "Lower Back",
            secondaryMuscles = listOf("Hamstrings", "Glutes"),
            difficulty = Difficulty.INTERMEDIATE,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_HIP, L_KNEE),
                mirrorJoints = JointTriplet(R_SHOULDER, R_HIP, R_KNEE),
                downThreshold = 80f, upThreshold = 150f
            ),
            formTips = listOf(
                "Stand with feet shoulder-width, hands behind head",
                "Hinge at the hips, pushing them backward",
                "Keep your back straight with a slight natural arch",
                "Lower until torso is near parallel to the floor",
                "Drive hips forward to return to standing"
            ),
            scoreType = ScoreType.HIP_HINGE,
            musclesWorked = listOf(MuscleGroup.LOWER_BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)
        ),

        // ── SHOULDERS ─────────────────────────────────────────────────────────
        ExerciseDefinition(
            name = "Pike Push-ups",
            emoji = "🔻",
            category = ExerciseCategory.SHOULDERS,
            primaryMuscle = "Shoulders",
            secondaryMuscles = listOf("Triceps", "Upper Chest"),
            difficulty = Difficulty.INTERMEDIATE,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_ELBOW, L_WRIST),
                mirrorJoints = JointTriplet(R_SHOULDER, R_ELBOW, R_WRIST),
                downThreshold = 90f, upThreshold = 150f
            ),
            formTips = listOf(
                "Form an inverted V-shape — downward dog position",
                "Hands shoulder-width apart, hips high toward ceiling",
                "Bend elbows to lower your head toward the floor",
                "Focus on loading the shoulders, not the chest",
                "Press back to the inverted V position"
            ),
            scoreType = ScoreType.ELBOW_PUSH,
            musclesWorked = listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, MuscleGroup.CHEST)
        ),
        ExerciseDefinition(
            name = "Shoulder Taps",
            emoji = "👆",
            category = ExerciseCategory.SHOULDERS,
            primaryMuscle = "Shoulders",
            secondaryMuscles = listOf("Core", "Chest"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_HIP, L_SHOULDER, L_ELBOW),
                mirrorJoints = null,
                downThreshold = 155f, upThreshold = 60f,
                invertPhase = true
            ),
            formTips = listOf(
                "Start in high plank position, shoulders over wrists",
                "Tap your left hand to your right shoulder, then alternate",
                "Keep hips level — don't rotate your torso",
                "Feet wider than hip-width for extra stability",
                "Move with control, not speed"
            ),
            scoreType = ScoreType.DYNAMIC_CARDIO,
            musclesWorked = listOf(MuscleGroup.SHOULDERS, MuscleGroup.ABS, MuscleGroup.CHEST)
        ),

        // ── BICEPS ────────────────────────────────────────────────────────────
        ExerciseDefinition(
            name = "Bicep Curls",
            emoji = "💪",
            category = ExerciseCategory.BICEPS,
            primaryMuscle = "Biceps",
            secondaryMuscles = listOf("Forearms"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_ELBOW, L_WRIST),
                mirrorJoints = JointTriplet(R_SHOULDER, R_ELBOW, R_WRIST),
                downThreshold = 155f, upThreshold = 50f,
                invertPhase = true
            ),
            formTips = listOf(
                "Stand tall, arms hanging at sides holding weights",
                "Pin your elbows to your sides throughout",
                "Curl the weight up toward your shoulders",
                "Squeeze at the top for 1 second",
                "Lower slowly — the eccentric phase builds muscle too"
            ),
            scoreType = ScoreType.ELBOW_PULL,
            musclesWorked = listOf(MuscleGroup.BICEPS)
        ),
        ExerciseDefinition(
            name = "Hammer Curls",
            emoji = "🔨",
            category = ExerciseCategory.BICEPS,
            primaryMuscle = "Biceps (brachialis)",
            secondaryMuscles = listOf("Forearms"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_ELBOW, L_WRIST),
                mirrorJoints = JointTriplet(R_SHOULDER, R_ELBOW, R_WRIST),
                downThreshold = 155f, upThreshold = 55f,
                invertPhase = true
            ),
            formTips = listOf(
                "Hold weights with a neutral grip — palms facing in",
                "Keep elbows stationary at your sides",
                "Curl to shoulder height, thumb pointing up",
                "Targets brachialis and brachioradialis",
                "Lower with control — full range of motion"
            ),
            scoreType = ScoreType.ELBOW_PULL,
            musclesWorked = listOf(MuscleGroup.BICEPS)
        ),

        // ── TRICEPS ───────────────────────────────────────────────────────────
        ExerciseDefinition(
            name = "Tricep Dips",
            emoji = "🪑",
            category = ExerciseCategory.TRICEPS,
            primaryMuscle = "Triceps",
            secondaryMuscles = listOf("Shoulders", "Chest"),
            difficulty = Difficulty.INTERMEDIATE,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_ELBOW, L_WRIST),
                mirrorJoints = JointTriplet(R_SHOULDER, R_ELBOW, R_WRIST),
                downThreshold = 90f, upThreshold = 150f
            ),
            formTips = listOf(
                "Place hands on a sturdy chair or bench behind you",
                "Slide your hips off the edge, legs extended or bent",
                "Lower body by bending elbows to 90°",
                "Keep elbows pointing straight back, not flaring out",
                "Press back up to full arm extension"
            ),
            scoreType = ScoreType.ELBOW_PUSH,
            musclesWorked = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS, MuscleGroup.CHEST)
        ),
        ExerciseDefinition(
            name = "Tricep Extensions",
            emoji = "🔼",
            category = ExerciseCategory.TRICEPS,
            primaryMuscle = "Triceps",
            secondaryMuscles = listOf("Shoulders"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_WRIST, L_ELBOW, L_SHOULDER),
                mirrorJoints = JointTriplet(R_WRIST, R_ELBOW, R_SHOULDER),
                downThreshold = 75f, upThreshold = 155f
            ),
            formTips = listOf(
                "Hold weight(s) overhead with arms fully extended",
                "Lower the weight behind your head, elbows pointing up",
                "Keep upper arms stationary — only forearms move",
                "Extend fully at the top, squeezing the triceps",
                "Avoid flaring elbows — keep them close to your head"
            ),
            scoreType = ScoreType.ELBOW_OVERHEAD,
            musclesWorked = listOf(MuscleGroup.TRICEPS)
        ),

        // ── CORE ──────────────────────────────────────────────────────────────
        ExerciseDefinition(
            name = "Plank",
            emoji = "🧘",
            category = ExerciseCategory.CORE,
            primaryMuscle = "Core",
            secondaryMuscles = listOf("Shoulders", "Glutes"),
            difficulty = Difficulty.BEGINNER,
            isTimeBased = true,
            repConfig = null,
            formTips = listOf(
                "Place forearms on the floor, elbows under shoulders",
                "Body forms a straight line from head to heels",
                "Engage your core — don't let your hips sag or rise",
                "Keep your neck neutral, gaze at the floor",
                "Breathe steadily and hold the position"
            ),
            scoreType = ScoreType.STATIC_HOLD,
            musclesWorked = listOf(MuscleGroup.ABS, MuscleGroup.SHOULDERS, MuscleGroup.GLUTES)
        ),
        ExerciseDefinition(
            name = "Crunches",
            emoji = "🤸",
            category = ExerciseCategory.CORE,
            primaryMuscle = "Abs",
            secondaryMuscles = listOf("Hip Flexors"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_HIP, L_KNEE),
                mirrorJoints = JointTriplet(R_SHOULDER, R_HIP, R_KNEE),
                downThreshold = 140f, upThreshold = 110f,
                invertPhase = true
            ),
            formTips = listOf(
                "Lie on your back, knees bent, feet flat on floor",
                "Place hands behind head — don't pull on your neck",
                "Lift shoulder blades off the floor using your abs",
                "Focus on contracting abs, not just moving your head",
                "Lower slowly with control each rep"
            ),
            scoreType = ScoreType.TRUNK_FLEXION,
            musclesWorked = listOf(MuscleGroup.ABS)
        ),
        ExerciseDefinition(
            name = "Sit-ups",
            emoji = "🙆",
            category = ExerciseCategory.CORE,
            primaryMuscle = "Abs",
            secondaryMuscles = listOf("Hip Flexors", "Lower Back"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_HIP, L_KNEE),
                mirrorJoints = JointTriplet(R_SHOULDER, R_HIP, R_KNEE),
                downThreshold = 150f, upThreshold = 70f,
                invertPhase = true
            ),
            formTips = listOf(
                "Lie on your back, knees bent at 90°, feet flat",
                "Cross arms over chest or place hands behind head",
                "Curl your torso up until elbows touch knees",
                "Keep feet flat on the floor throughout",
                "Lower your back down vertebra by vertebra"
            ),
            scoreType = ScoreType.TRUNK_FLEXION,
            musclesWorked = listOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES)
        ),
        ExerciseDefinition(
            name = "Leg Raises",
            emoji = "🦵",
            category = ExerciseCategory.CORE,
            primaryMuscle = "Lower Abs",
            secondaryMuscles = listOf("Hip Flexors"),
            difficulty = Difficulty.INTERMEDIATE,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_HIP, L_KNEE),
                mirrorJoints = JointTriplet(R_SHOULDER, R_HIP, R_KNEE),
                downThreshold = 155f, upThreshold = 80f,
                invertPhase = true
            ),
            formTips = listOf(
                "Lie flat on your back, hands by your sides or under hips",
                "Keep legs straight — a slight knee bend is fine",
                "Raise legs to 90° — perpendicular to the floor",
                "Don't let legs swing — control every inch",
                "Lower slowly, stopping just before heels touch the floor"
            ),
            scoreType = ScoreType.LEG_RAISE,
            musclesWorked = listOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES)
        ),
        ExerciseDefinition(
            name = "Mountain Climbers",
            emoji = "🧗",
            category = ExerciseCategory.CORE,
            primaryMuscle = "Core",
            secondaryMuscles = listOf("Hip Flexors", "Chest", "Shoulders"),
            difficulty = Difficulty.INTERMEDIATE,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_HIP, L_KNEE, L_ANKLE),
                mirrorJoints = JointTriplet(R_HIP, R_KNEE, R_ANKLE),
                downThreshold = 55f, upThreshold = 140f,
                invertPhase = true,
                // Alternating: one knee drives in at a time. Min angle = the knee that's furthest
                // forward/contracted, so we count correctly on EITHER side.
                symmetric = false
            ),
            formTips = listOf(
                "Start in high plank — shoulders over wrists",
                "Drive one knee toward your chest explosively",
                "Quickly switch legs in a running motion",
                "Keep hips level — don't bounce them up",
                "The faster you go, the more cardio benefit"
            ),
            scoreType = ScoreType.DYNAMIC_CARDIO,
            musclesWorked = listOf(MuscleGroup.ABS, MuscleGroup.FULL_BODY)
        ),
        ExerciseDefinition(
            name = "Bicycle Crunches",
            emoji = "🚲",
            category = ExerciseCategory.CORE,
            primaryMuscle = "Obliques",
            secondaryMuscles = listOf("Abs", "Hip Flexors"),
            difficulty = Difficulty.INTERMEDIATE,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_HIP, L_KNEE, L_ANKLE),
                mirrorJoints = JointTriplet(R_HIP, R_KNEE, R_ANKLE),
                downThreshold = 55f, upThreshold = 140f,
                invertPhase = true,
                // Alternating: one knee drives in at a time — use min angle to catch either side.
                symmetric = false
            ),
            formTips = listOf(
                "Lie on your back, hands behind head",
                "Bring opposite elbow to opposite knee each rep",
                "Keep the other leg extended, off the floor",
                "Rotate your torso — it's not just a crunch",
                "Don't pull on your neck with your hands"
            ),
            scoreType = ScoreType.TRUNK_FLEXION,
            musclesWorked = listOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES)
        ),

        // ── LEGS ──────────────────────────────────────────────────────────────
        ExerciseDefinition(
            name = "Squats",
            emoji = "🦵",
            category = ExerciseCategory.LEGS,
            primaryMuscle = "Quads",
            secondaryMuscles = listOf("Glutes", "Hamstrings"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_HIP, L_KNEE, L_ANKLE),
                mirrorJoints = JointTriplet(R_HIP, R_KNEE, R_ANKLE),
                downThreshold = 100f, upThreshold = 155f
            ),
            formTips = listOf(
                "Stand with feet shoulder-width apart, toes slightly out",
                "Keep your chest up and back straight throughout",
                "Lower until thighs are parallel to the floor",
                "Drive through your heels to stand back up",
                "Keep knees tracking over your toes, not caving in"
            ),
            scoreType = ScoreType.KNEE_FLEXION,
            musclesWorked = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS)
        ),
        ExerciseDefinition(
            name = "Jump Squats",
            emoji = "⬆️",
            category = ExerciseCategory.LEGS,
            primaryMuscle = "Quads",
            secondaryMuscles = listOf("Glutes", "Calves"),
            difficulty = Difficulty.INTERMEDIATE,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_HIP, L_KNEE, L_ANKLE),
                mirrorJoints = JointTriplet(R_HIP, R_KNEE, R_ANKLE),
                downThreshold = 100f, upThreshold = 160f
            ),
            formTips = listOf(
                "Start standing, feet shoulder-width apart",
                "Squat down until thighs are parallel to the ground",
                "Explode upward, jumping off the floor",
                "Land softly on balls of feet, go immediately into next squat",
                "Use arms for momentum — swing up as you jump"
            ),
            scoreType = ScoreType.KNEE_FLEXION,
            musclesWorked = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
        ),
        ExerciseDefinition(
            name = "Sumo Squats",
            emoji = "🤼",
            category = ExerciseCategory.LEGS,
            primaryMuscle = "Inner Thighs",
            secondaryMuscles = listOf("Glutes", "Quads"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_HIP, L_KNEE, L_ANKLE),
                mirrorJoints = JointTriplet(R_HIP, R_KNEE, R_ANKLE),
                downThreshold = 95f, upThreshold = 155f
            ),
            formTips = listOf(
                "Take a wider than shoulder-width stance, toes pointing out",
                "Keep chest up and back straight",
                "Lower straight down — knees track over toes",
                "Squeeze inner thighs and glutes as you rise",
                "Great for targeting adductors and glutes"
            ),
            scoreType = ScoreType.KNEE_FLEXION,
            musclesWorked = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS)
        ),
        ExerciseDefinition(
            name = "Wall Sit",
            emoji = "🪑",
            category = ExerciseCategory.LEGS,
            primaryMuscle = "Quads",
            secondaryMuscles = listOf("Glutes", "Calves"),
            difficulty = Difficulty.BEGINNER,
            isTimeBased = true,
            repConfig = null,
            formTips = listOf(
                "Stand with your back flat against a wall",
                "Slide down until thighs are parallel to the floor",
                "Knees directly above ankles — 90° angle",
                "Keep your back flat against the wall throughout",
                "Hold as long as possible — breathe steadily"
            ),
            scoreType = ScoreType.STATIC_HOLD,
            musclesWorked = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES)
        ),
        ExerciseDefinition(
            name = "Lunges",
            emoji = "🏃",
            category = ExerciseCategory.LEGS,
            primaryMuscle = "Quads",
            secondaryMuscles = listOf("Glutes", "Hamstrings"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_HIP, L_KNEE, L_ANKLE),
                mirrorJoints = JointTriplet(R_HIP, R_KNEE, R_ANKLE),
                downThreshold = 100f, upThreshold = 155f,
                // Lunges are asymmetric: front knee bends deeply while back knee is extended.
                // Use the min (most bent) knee to correctly detect each lunge rep.
                symmetric = false
            ),
            formTips = listOf(
                "Stand tall, step forward with one foot",
                "Lower your back knee toward the floor",
                "Keep your front knee directly above your ankle",
                "Push through your front heel to return to start",
                "Alternate legs each rep and keep your torso upright"
            ),
            scoreType = ScoreType.KNEE_FLEXION,
            musclesWorked = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS)
        ),
        ExerciseDefinition(
            name = "Reverse Lunges",
            emoji = "↩️",
            category = ExerciseCategory.LEGS,
            primaryMuscle = "Glutes",
            secondaryMuscles = listOf("Quads", "Hamstrings"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_HIP, L_KNEE, L_ANKLE),
                mirrorJoints = JointTriplet(R_HIP, R_KNEE, R_ANKLE),
                downThreshold = 100f, upThreshold = 155f,
                symmetric = false
            ),
            formTips = listOf(
                "Stand tall, step backward with one foot",
                "Lower your back knee toward the floor",
                "Front knee stays over front ankle — no forward lean",
                "Push through your front heel to return",
                "Easier on the knees than forward lunges"
            ),
            scoreType = ScoreType.KNEE_FLEXION,
            musclesWorked = listOf(MuscleGroup.GLUTES, MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS)
        ),

        // ── GLUTES ────────────────────────────────────────────────────────────
        ExerciseDefinition(
            name = "Glute Bridges",
            emoji = "🌉",
            category = ExerciseCategory.GLUTES,
            primaryMuscle = "Glutes",
            secondaryMuscles = listOf("Hamstrings", "Core"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_KNEE, L_HIP, L_SHOULDER),
                mirrorJoints = JointTriplet(R_KNEE, R_HIP, R_SHOULDER),
                downThreshold = 120f, upThreshold = 155f
            ),
            formTips = listOf(
                "Lie on your back, knees bent, feet flat on floor",
                "Drive through your heels to lift your hips",
                "Squeeze glutes hard at the top",
                "Body forms a straight line from knees to shoulders",
                "Lower slowly — don't let hips drop all the way"
            ),
            scoreType = ScoreType.HIP_EXTENSION,
            musclesWorked = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.ABS)
        ),
        ExerciseDefinition(
            name = "Donkey Kicks",
            emoji = "🦵",
            category = ExerciseCategory.GLUTES,
            primaryMuscle = "Glutes",
            secondaryMuscles = listOf("Hamstrings", "Core"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_HIP, L_KNEE),
                mirrorJoints = null,
                downThreshold = 95f, upThreshold = 145f,
                invertPhase = true
            ),
            formTips = listOf(
                "Start on all fours — hands under shoulders, knees under hips",
                "Keeping knee bent at 90°, lift one leg up behind you",
                "Squeeze glutes hard at the top of the movement",
                "Hip is the primary hinge — avoid rotating torso",
                "Complete all reps on one side before switching"
            ),
            scoreType = ScoreType.HIP_EXTENSION,
            musclesWorked = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS)
        ),
        ExerciseDefinition(
            name = "Hip Thrusts",
            emoji = "🚀",
            category = ExerciseCategory.GLUTES,
            primaryMuscle = "Glutes",
            secondaryMuscles = listOf("Hamstrings", "Quads"),
            difficulty = Difficulty.INTERMEDIATE,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_KNEE, L_HIP, L_SHOULDER),
                mirrorJoints = JointTriplet(R_KNEE, R_HIP, R_SHOULDER),
                downThreshold = 100f, upThreshold = 155f
            ),
            formTips = listOf(
                "Sit with upper back resting on a bench or couch",
                "Feet flat, knees bent at 90°",
                "Drive through your heels, thrusting hips toward the ceiling",
                "Squeeze glutes hard at the top",
                "Lower hips until just above the floor and repeat"
            ),
            scoreType = ScoreType.HIP_EXTENSION,
            musclesWorked = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS)
        ),

        // ── CALVES ────────────────────────────────────────────────────────────
        ExerciseDefinition(
            name = "Calf Raises",
            emoji = "🦶",
            category = ExerciseCategory.CALVES,
            primaryMuscle = "Calves",
            secondaryMuscles = listOf("Ankles"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_KNEE, L_ANKLE, L_HEEL),
                mirrorJoints = JointTriplet(R_KNEE, R_ANKLE, R_HEEL),
                downThreshold = 85f, upThreshold = 115f
            ),
            formTips = listOf(
                "Stand upright, feet hip-width apart",
                "Rise up onto the balls of your feet, lifting heels",
                "Hold for 1 second at the top, squeezing calves",
                "Lower slowly — the stretch at the bottom is important",
                "Stand on the edge of a step for a greater range of motion"
            ),
            scoreType = ScoreType.CALF_RAISE,
            musclesWorked = listOf(MuscleGroup.CALVES)
        ),

        // ── FULL BODY ──────────────────────────────────────────────────────────
        ExerciseDefinition(
            name = "Burpees",
            emoji = "🔥",
            category = ExerciseCategory.FULL_BODY,
            primaryMuscle = "Full Body",
            secondaryMuscles = listOf("Chest", "Legs", "Core"),
            difficulty = Difficulty.ADVANCED,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_SHOULDER, L_HIP, L_KNEE),
                mirrorJoints = null,
                downThreshold = 100f, upThreshold = 155f
            ),
            formTips = listOf(
                "Start standing, then squat and place hands on floor",
                "Jump feet back into plank/push-up position",
                "Optionally do one push-up here",
                "Jump feet back toward your hands",
                "Explode upward, jumping with arms overhead"
            ),
            scoreType = ScoreType.DYNAMIC_CARDIO,
            musclesWorked = listOf(MuscleGroup.FULL_BODY, MuscleGroup.CHEST, MuscleGroup.QUADS)
        ),
        ExerciseDefinition(
            name = "High Knees",
            emoji = "🏃",
            category = ExerciseCategory.FULL_BODY,
            primaryMuscle = "Hip Flexors",
            secondaryMuscles = listOf("Abs", "Quads"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_HIP, L_KNEE, L_ANKLE),
                mirrorJoints = JointTriplet(R_HIP, R_KNEE, R_ANKLE),
                downThreshold = 80f, upThreshold = 145f,
                invertPhase = true,
                // Alternating: one knee drives up at a time. Min angle = the knee that's highest,
                // so a rep is counted on EITHER the left or right raise.
                symmetric = false
            ),
            formTips = listOf(
                "Run in place, driving knees up to hip height",
                "Pump arms in sync with your legs",
                "Land on the balls of your feet, not heels",
                "Keep your core tight and chest up",
                "Aim to bring each knee above 90° from the hip"
            ),
            scoreType = ScoreType.DYNAMIC_CARDIO,
            musclesWorked = listOf(MuscleGroup.QUADS, MuscleGroup.ABS, MuscleGroup.FULL_BODY)
        ),
        ExerciseDefinition(
            name = "Jumping Jacks",
            emoji = "⭐",
            category = ExerciseCategory.FULL_BODY,
            primaryMuscle = "Full Body",
            secondaryMuscles = listOf("Shoulders", "Calves"),
            difficulty = Difficulty.BEGINNER,
            repConfig = RepConfig(
                primaryJoints = JointTriplet(L_HIP, L_SHOULDER, L_ELBOW),
                mirrorJoints = JointTriplet(R_HIP, R_SHOULDER, R_ELBOW),
                downThreshold = 125f, upThreshold = 55f,
                invertPhase = true
            ),
            formTips = listOf(
                "Start standing, feet together, arms at your sides",
                "Jump feet out while raising arms overhead",
                "Jump feet back together while lowering arms",
                "Keep a slight bend in knees when landing",
                "Maintain a steady rhythm — tempo is everything"
            ),
            scoreType = ScoreType.DYNAMIC_CARDIO,
            musclesWorked = listOf(MuscleGroup.FULL_BODY, MuscleGroup.SHOULDERS, MuscleGroup.CALVES)
        ),
        ExerciseDefinition(
            name = "Bear Crawl",
            emoji = "🐻",
            category = ExerciseCategory.FULL_BODY,
            primaryMuscle = "Core",
            secondaryMuscles = listOf("Shoulders", "Quads"),
            difficulty = Difficulty.INTERMEDIATE,
            isTimeBased = true,
            repConfig = null,
            formTips = listOf(
                "Start on all fours, knees hovering 1–2 inches above ground",
                "Move right hand and left foot forward simultaneously",
                "Then left hand and right foot — opposite limbs move together",
                "Keep your back flat and hips low — don't let them rise",
                "Quality beats speed — move deliberately"
            ),
            scoreType = ScoreType.STATIC_HOLD,
            musclesWorked = listOf(MuscleGroup.ABS, MuscleGroup.SHOULDERS, MuscleGroup.QUADS)
        )
    )

    fun getByName(name: String): ExerciseDefinition? = all.find { it.name == name }

    fun getByCategory(category: ExerciseCategory): List<ExerciseDefinition> =
        all.filter { it.category == category }

    fun getNames(): List<String> = all.map { it.name }

    fun getCategories(): List<ExerciseCategory> =
        ExerciseCategory.entries.filter { cat -> all.any { it.category == cat } }

    fun getPopular(): List<ExerciseDefinition> =
        listOf("Squats", "Push-ups", "Lunges", "Plank", "Burpees", "High Knees")
            .mapNotNull { getByName(it) }
}
