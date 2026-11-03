package com.jsf.app.pravin.data.repository

import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.*
import com.jsf.app.pravin.data.remote.FirestoreCommunityDataSource
import kotlinx.coroutines.flow.Flow

class CommunityRepository(
    private val db: FitnessDatabase,
    private val fs: FirestoreCommunityDataSource = FirestoreCommunityDataSource()
) {

    // ── Servers (Firestore) ───────────────────────────────────────────────────
    val publicServers = fs.getPublicServers()
    fun getServerById(id: String) = fs.getServerById(id)
    fun getServersByOwner(userId: String) = fs.getServersByOwner(userId)
    fun searchServers(query: String) = fs.searchPublicServers(query)
    suspend fun saveServer(server: Server) = fs.saveServer(server)
    suspend fun deleteServer(server: Server) = fs.deleteServer(server)

    // ── Membership (Firestore) ────────────────────────────────────────────────
    fun getMembersOfServer(serverId: String) = fs.getMembersOfServer(serverId)
    fun getServersForUser(userId: String) = fs.getServersForUser(userId)
    fun getMembership(serverId: String, userId: String) = fs.getMembership(serverId, userId)
    suspend fun getUserRole(serverId: String, userId: String) = fs.getUserRole(serverId, userId)
    suspend fun joinServer(member: ServerMember) = fs.joinServer(member)
    suspend fun leaveServer(serverId: String, userId: String) = fs.leaveServer(serverId, userId)
    suspend fun updateMemberRole(serverId: String, userId: String, role: String) =
        fs.updateMemberRole(serverId, userId, role)

    // ── Channels (Firestore) ──────────────────────────────────────────────────
    fun getChannelsForServer(serverId: String) = fs.getChannelsForServer(serverId)
    fun getChannelById(id: String) = fs.getChannelById(id)
    suspend fun saveChannel(channel: Channel) = fs.saveChannel(channel)
    suspend fun deleteChannel(channel: Channel) = fs.deleteChannel(channel)

    // ── Messages (Firestore) ──────────────────────────────────────────────────
    fun getMessagesForChannel(channelId: String) = fs.getMessagesForChannel(channelId)
    fun getRecentMessages(channelId: String, limit: Int = 50) = fs.getRecentMessages(channelId, limit)
    suspend fun sendMessage(message: Message) = fs.sendMessage(message)
    suspend fun deleteMessage(messageId: String) = fs.deleteMessage(messageId)
    suspend fun editMessage(messageId: String, content: String, editedAt: Long) =
        fs.editMessage(messageId, content, editedAt)

    // ── Workout Routines (Room — server-scoped, local for now) ────────────────
    fun getRoutinesForServer(serverId: String) = db.workoutRoutineDao().getRoutinesForServer(serverId)
    fun getRoutineById(id: String) = db.workoutRoutineDao().getRoutineById(id)
    fun getUpcomingRoutines(serverId: String, fromDate: Long) = db.workoutRoutineDao().getUpcomingRoutines(serverId, fromDate)
    suspend fun saveRoutine(routine: WorkoutRoutine) = db.workoutRoutineDao().upsertRoutine(routine)
    fun getExercisesForRoutine(routineId: String) = db.routineExerciseDao().getExercisesForRoutine(routineId)
    suspend fun saveRoutineExercises(exercises: List<RoutineExercise>) =
        db.routineExerciseDao().upsertExercises(exercises)

    // ── Challenges (Room) ─────────────────────────────────────────────────────
    fun getChallengesForServer(serverId: String) = db.challengeDao().getChallengesForServer(serverId)
    fun getChallengeById(id: String) = db.challengeDao().getChallengeById(id)
    fun getChallengeLeaderboard(challengeId: String) = db.challengeDao().getLeaderboard(challengeId)
    fun getUserChallengeProgress(challengeId: String, userId: String) =
        db.challengeDao().getUserProgress(challengeId, userId)
    fun getUserChallenges(userId: String) = db.challengeDao().getUserChallenges(userId)
    suspend fun saveChallenge(challenge: Challenge) = db.challengeDao().upsertChallenge(challenge)
    suspend fun saveProgress(progress: ChallengeProgress) = db.challengeDao().upsertProgress(progress)

    // ── Posts (Room) ──────────────────────────────────────────────────────────
    fun getPostsForChannel(channelId: String) = db.postDao().getPostsForChannel(channelId)
    fun getRecentPostsForServer(serverId: String, limit: Int = 50) = db.postDao().getRecentPostsForServer(serverId, limit)
    fun getPostsByUser(userId: String) = db.postDao().getPostsByUser(userId)
    suspend fun savePost(post: Post) = db.postDao().upsertPost(post)
    suspend fun deletePost(post: Post) = db.postDao().deletePost(post)
    suspend fun updatePostLikes(postId: String, delta: Int) = db.postDao().updateLikeCount(postId, delta)

    // ── Meals (Room) ──────────────────────────────────────────────────────────
    fun getMealPostsForChannel(channelId: String) = db.mealPostDao().getMealPostsForChannel(channelId)
    fun getRecentMealPosts(serverId: String) = db.mealPostDao().getRecentMealPosts(serverId)
    fun getMealsByUser(userId: String) = db.mealPostDao().getMealsByUser(userId)
    suspend fun saveMealPost(mealPost: MealPost) = db.mealPostDao().upsertMealPost(mealPost)

    // ── Notifications (Room) ──────────────────────────────────────────────────
    fun getNotificationsForUser(userId: String) = db.notificationDao().getNotificationsForUser(userId)
    fun getUnreadNotifications(userId: String) = db.notificationDao().getUnreadNotifications(userId)
    fun getUnreadCount(userId: String) = db.notificationDao().getUnreadCount(userId)
    suspend fun insertNotification(notification: AppNotification) =
        db.notificationDao().insertNotification(notification)
    suspend fun markNotificationRead(id: Long) = db.notificationDao().markAsRead(id)
    suspend fun markAllNotificationsRead(userId: String) = db.notificationDao().markAllAsRead(userId)

    // ── Invites (Room) ────────────────────────────────────────────────────────
    fun getInvitesForServer(serverId: String) = db.inviteDao().getInvitesForServer(serverId)
    suspend fun getInviteByCode(code: String) = db.inviteDao().getInviteByCode(code)
    suspend fun saveInvite(invite: Invite) = db.inviteDao().upsertInvite(invite)
    suspend fun revokeInvite(code: String) = db.inviteDao().revokeInvite(code)
    suspend fun useInvite(code: String) = db.inviteDao().incrementUseCount(code)

    // ── Live Sessions (Room) ──────────────────────────────────────────────────
    fun getLiveSessionsForServer(serverId: String) = db.liveSessionDao().getLiveSessionsForServer(serverId)
    fun getActiveLiveSession(serverId: String) = db.liveSessionDao().getActiveLiveSession(serverId)
    fun getUpcomingLiveSessions(serverId: String, now: Long) = db.liveSessionDao().getUpcomingSessions(serverId, now)
    fun getLiveSessionById(id: String) = db.liveSessionDao().getLiveSessionById(id)
    suspend fun saveLiveSession(session: LiveSession) = db.liveSessionDao().upsertLiveSession(session)
    suspend fun startLiveSession(sessionId: String, startedAt: Long) =
        db.liveSessionDao().startSession(sessionId, LiveSession.Status.LIVE, startedAt)
    suspend fun endLiveSession(sessionId: String, endedAt: Long) =
        db.liveSessionDao().endSession(sessionId, endedAt)

    // ── Achievements (Room) ───────────────────────────────────────────────────
    fun getAchievementsForUser(userId: String) = db.achievementDao().getAchievementsForUser(userId)
    fun getAchievementCount(userId: String) = db.achievementDao().getAchievementCount(userId)
    suspend fun grantAchievement(achievement: Achievement) =
        db.achievementDao().insertAchievement(achievement)

    // ── Reactions (Room) ──────────────────────────────────────────────────────
    fun getReactionsForPost(postId: String) = db.reactionDao().getReactionsForPost(postId)
    suspend fun addReaction(reaction: Reaction) = db.reactionDao().insertReaction(reaction)
    suspend fun removeReaction(postId: String, userId: String, emoji: String) =
        db.reactionDao().removeReaction(postId, userId, emoji)

    // ── Comments (Room) ───────────────────────────────────────────────────────
    fun getCommentsForPost(postId: String) = db.commentDao().getCommentsForPost(postId)
    fun getCommentCount(postId: String) = db.commentDao().getCommentCount(postId)
    suspend fun addComment(comment: Comment) = db.commentDao().insertComment(comment)
    suspend fun deleteComment(comment: Comment) = db.commentDao().deleteComment(comment)

    // ── Roles (Room) ──────────────────────────────────────────────────────────
    fun getRolesForServer(serverId: String) = db.serverRoleDao().getRolesForServer(serverId)
    suspend fun saveRole(role: ServerRole) = db.serverRoleDao().upsertRole(role)
    suspend fun saveDefaultRoles(serverId: String) {
        val defaultRoles = listOf(
            ServerRole(serverId = serverId, name = ServerRole.OWNER, color = "#FFD700", position = 0, permissions = "[]"),
            ServerRole(serverId = serverId, name = ServerRole.ADMIN, color = "#FF6B6B", position = 1, permissions = "[]"),
            ServerRole(serverId = serverId, name = ServerRole.MODERATOR, color = "#4ECDC4", position = 2, permissions = "[]"),
            ServerRole(serverId = serverId, name = ServerRole.COACH, color = "#45B7D1", position = 3, permissions = "[]"),
            ServerRole(serverId = serverId, name = ServerRole.MEMBER, color = "#AAAAAA", position = 4, isDefault = true, permissions = "[]")
        )
        db.serverRoleDao().upsertRoles(defaultRoles)
    }
}
