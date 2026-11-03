package com.jsf.app.pravin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jsf.app.pravin.data.model.*

@Database(
    entities = [
        UserProfile::class,
        WorkoutSession::class,
        Server::class,
        ServerMember::class,
        Channel::class,
        Message::class,
        WorkoutRoutine::class,
        RoutineExercise::class,
        Challenge::class,
        ChallengeProgress::class,
        Post::class,
        MealPost::class,
        AppNotification::class,
        Invite::class,
        LiveSession::class,
        Achievement::class,
        Reaction::class,
        Comment::class,
        ServerRole::class,
        RepFormRecord::class
    ],
    version = 5,
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun serverDao(): ServerDao
    abstract fun serverMemberDao(): ServerMemberDao
    abstract fun channelDao(): ChannelDao
    abstract fun messageDao(): MessageDao
    abstract fun workoutRoutineDao(): WorkoutRoutineDao
    abstract fun routineExerciseDao(): RoutineExerciseDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun postDao(): PostDao
    abstract fun mealPostDao(): MealPostDao
    abstract fun notificationDao(): NotificationDao
    abstract fun inviteDao(): InviteDao
    abstract fun liveSessionDao(): LiveSessionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun reactionDao(): ReactionDao
    abstract fun commentDao(): CommentDao
    abstract fun serverRoleDao(): ServerRoleDao
    abstract fun repFormRecordDao(): RepFormRecordDao

    companion object {
        @Volatile private var INSTANCE: FitnessDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Extend existing tables
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN bio TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN avatarUrl TEXT NOT NULL DEFAULT ''")

                database.execSQL("ALTER TABLE workout_sessions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE workout_sessions ADD COLUMN serverId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE workout_sessions ADD COLUMN routineId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE workout_sessions ADD COLUMN isPersonal INTEGER NOT NULL DEFAULT 1")

                // servers
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `servers` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `iconUrl` TEXT NOT NULL,
                        `bannerUrl` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `isPublic` INTEGER NOT NULL,
                        `memberCount` INTEGER NOT NULL,
                        `categories` TEXT NOT NULL,
                        `tags` TEXT NOT NULL,
                        `rules` TEXT NOT NULL,
                        `welcomeMessage` TEXT NOT NULL,
                        `accentColor` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // server_members
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `server_members` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `joinedAt` INTEGER NOT NULL,
                        `nickname` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_server_members_serverId_userId` ON `server_members` (`serverId`, `userId`)")

                // channels
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `channels` (
                        `id` TEXT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `isPublic` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // messages
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `messages` (
                        `id` TEXT NOT NULL,
                        `channelId` TEXT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `attachmentUrl` TEXT NOT NULL,
                        `attachmentType` TEXT NOT NULL,
                        `replyToId` TEXT NOT NULL,
                        `isEdited` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `editedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // workout_routines
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workout_routines` (
                        `id` TEXT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `scheduledDate` INTEGER NOT NULL,
                        `scheduledTime` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `estimatedDuration` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // routine_exercises
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `routine_exercises` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `routineId` TEXT NOT NULL,
                        `exerciseName` TEXT NOT NULL,
                        `sets` INTEGER NOT NULL,
                        `targetReps` INTEGER NOT NULL,
                        `targetDurationSeconds` INTEGER NOT NULL,
                        `restSeconds` INTEGER NOT NULL,
                        `instructions` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `isTimeBased` INTEGER NOT NULL
                    )
                """.trimIndent())

                // challenges
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `challenges` (
                        `id` TEXT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `rules` TEXT NOT NULL,
                        `targetExercise` TEXT NOT NULL,
                        `targetReps` INTEGER NOT NULL,
                        `targetDays` INTEGER NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `endDate` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `participantCount` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // challenge_progress
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `challenge_progress` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `challengeId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `completedDays` INTEGER NOT NULL,
                        `totalReps` INTEGER NOT NULL,
                        `currentStreak` INTEGER NOT NULL,
                        `lastActivityDate` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_challenge_progress_challengeId_userId` ON `challenge_progress` (`challengeId`, `userId`)")

                // posts
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `posts` (
                        `id` TEXT NOT NULL,
                        `channelId` TEXT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `mediaUrl` TEXT NOT NULL,
                        `workoutSessionId` INTEGER NOT NULL,
                        `exerciseType` TEXT NOT NULL,
                        `repCount` INTEGER NOT NULL,
                        `totalScore` REAL NOT NULL,
                        `likeCount` INTEGER NOT NULL,
                        `commentCount` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // meal_posts
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `meal_posts` (
                        `id` TEXT NOT NULL,
                        `channelId` TEXT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `mealType` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `aiAnalysis` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `likeCount` INTEGER NOT NULL,
                        `commentCount` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // notifications
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notifications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `data` TEXT NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())

                // invites
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invites` (
                        `id` TEXT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `maxUses` INTEGER NOT NULL,
                        `useCount` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL,
                        `isRevoked` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // live_sessions
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `live_sessions` (
                        `id` TEXT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `channelId` TEXT NOT NULL,
                        `routineId` TEXT NOT NULL,
                        `hostId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `scheduledAt` INTEGER NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `endedAt` INTEGER NOT NULL,
                        `participantCount` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // achievements
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `achievements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `earnedAt` INTEGER NOT NULL,
                        `serverId` TEXT NOT NULL
                    )
                """.trimIndent())

                // reactions
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `postId` TEXT NOT NULL,
                        `messageId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `emoji` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reactions_postId_userId_emoji` ON `reactions` (`postId`, `userId`, `emoji`)")

                // comments
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `comments` (
                        `id` TEXT NOT NULL,
                        `postId` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `likeCount` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // server_roles
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `server_roles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` TEXT NOT NULL,
                        `permissions` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_server_roles_serverId_name` ON `server_roles` (`serverId`, `name`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE workout_routines ADD COLUMN targetMuscleGroups TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN role TEXT NOT NULL DEFAULT 'athlete'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `rep_form_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `repNumber` INTEGER NOT NULL,
                        `exerciseType` TEXT NOT NULL,
                        `formErrors` TEXT NOT NULL,
                        `repScore` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_rep_form_records_sessionId` ON `rep_form_records` (`sessionId`)")
            }
        }

        fun getInstance(context: Context): FitnessDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitnessDatabase::class.java,
                    "fitness_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
            }
    }
}
