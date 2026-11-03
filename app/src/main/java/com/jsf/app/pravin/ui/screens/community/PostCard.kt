package com.jsf.app.pravin.ui.screens.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsf.app.pravin.data.model.Comment
import com.jsf.app.pravin.data.model.Post
import com.jsf.app.pravin.data.model.Reaction
import com.jsf.app.pravin.ui.theme.*
import java.util.concurrent.TimeUnit

@Composable
fun PostCard(
    post: Post,
    reactions: List<Reaction>,
    comments: List<Comment>,
    currentUserId: String,
    onToggleReaction: (emoji: String) -> Unit,
    onAddComment: (content: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author row
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuthorAvatar(authorId = post.authorId, size = 36)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (post.authorId == currentUserId) "You" else post.authorId.take(8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (post.authorId == currentUserId) NeonGreen else CyanPrimary
                    )
                    Text(
                        text = relativeTime(post.timestamp),
                        fontSize = 11.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
                PostTypeBadge(type = post.type)
            }

            Spacer(Modifier.height(10.dp))

            // Title (exerciseType for workout, else first line of content)
            val title = when {
                post.type == Post.PostType.WORKOUT && post.exerciseType.isNotBlank() -> post.exerciseType
                else -> null
            }
            title?.let {
                Text(
                    text = it,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
            }

            // Content
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = if (showMore) Int.MAX_VALUE else 4,
                    overflow = if (showMore) TextOverflow.Visible else TextOverflow.Ellipsis
                )
                if (post.content.length > 200 && !showMore) {
                    Text(
                        text = "Show more",
                        fontSize = 12.sp,
                        color = CyanPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { showMore = true }
                    )
                }
            }

            // Workout stats row
            if (post.type == Post.PostType.WORKOUT && (post.repCount > 0 || post.totalScore > 0f)) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (post.repCount > 0) {
                        StatChip(label = "${post.repCount} reps", color = NeonGreen)
                    }
                    if (post.totalScore > 0f) {
                        StatChip(label = "Score: ${post.totalScore.toInt()}", color = ScoreGold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DeepSpace, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            // Reaction row
            val reactionEmojis = listOf("💪", "🔥", "❤️", "🏆")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                reactionEmojis.forEach { emoji ->
                    val count = reactions.count { it.emoji == emoji }
                    val isActive = reactions.any { it.userId == currentUserId && it.emoji == emoji }
                    ReactionButton(
                        emoji = emoji,
                        count = count,
                        isActive = isActive,
                        onClick = { onToggleReaction(emoji) }
                    )
                }
                Spacer(Modifier.weight(1f))
                // Comment count
                Row(
                    modifier = Modifier.clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${comments.size}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Expanded comments section
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = DeepSpace, thickness = 1.dp)
                    Spacer(Modifier.height(8.dp))

                    if (comments.isEmpty()) {
                        Text(
                            "No comments yet",
                            fontSize = 12.sp,
                            color = TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        comments.forEach { comment ->
                            CommentRow(comment = comment, currentUserId = currentUserId)
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    // Comment composer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Add a comment...", color = TextSecondary, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = CardDark.copy(alpha = 0f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = CyanPrimary,
                                focusedContainerColor = DeepSpace,
                                unfocusedContainerColor = DeepSpace
                            ),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                onAddComment(commentText)
                                commentText = ""
                            },
                            enabled = commentText.isNotBlank(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Post comment",
                                tint = if (commentText.isNotBlank()) CyanPrimary else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionButton(emoji: String, count: Int, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) CyanPrimary.copy(alpha = 0.2f) else DeepSpace,
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 14.sp)
            if (count > 0) {
                Spacer(Modifier.width(3.dp))
                Text(
                    "$count",
                    fontSize = 11.sp,
                    color = if (isActive) CyanPrimary else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment, currentUserId: String) {
    Row(verticalAlignment = Alignment.Top) {
        AuthorAvatar(authorId = comment.authorId, size = 24)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = if (comment.authorId == currentUserId) "You" else comment.authorId.take(8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (comment.authorId == currentUserId) NeonGreen else CyanPrimary
            )
            Text(text = comment.content, fontSize = 13.sp, color = TextPrimary)
        }
    }
}

@Composable
fun AuthorAvatar(authorId: String, size: Int) {
    val initial = authorId.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val color = avatarColor(authorId)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            fontSize = (size * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun PostTypeBadge(type: String) {
    val (label, color) = when (type) {
        Post.PostType.WORKOUT -> Pair("Workout", NeonGreen)
        Post.PostType.MEAL -> Pair("Meal", OrangeWarning)
        Post.PostType.PROGRESS_PHOTO -> Pair("Progress", ScoreGold)
        Post.PostType.ACHIEVEMENT -> Pair("Achievement", ScoreGold)
        else -> return
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StatChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun avatarColor(id: String): Color {
    val colors = listOf(CyanPrimary, NeonGreen, OrangeWarning, ScoreGold, Color(0xFF9B59B6), RedDanger)
    return colors[Math.abs(id.hashCode()) % colors.size]
}

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
}
