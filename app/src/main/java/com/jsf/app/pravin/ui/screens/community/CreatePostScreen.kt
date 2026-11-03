package com.jsf.app.pravin.ui.screens.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.model.Post
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    channelId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: CommunityFeedViewModel = viewModel(
        factory = CommunityFeedViewModel.factory(app, channelId)
    )

    var selectedType by remember { mutableStateOf(Post.PostType.TEXT) }
    var content by remember { mutableStateOf("") }
    var exerciseType by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Text("New Post", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (content.isBlank()) {
                                error = "Content cannot be empty"
                            } else {
                                viewModel.createPost(
                                    content = content,
                                    type = selectedType,
                                    exerciseType = exerciseType
                                )
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = CyanPrimary)
                    ) {
                        Text("Post", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Post type selector
            Text("Post Type", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PostTypeChip(
                    label = "Text",
                    icon = Icons.Default.TextFields,
                    selected = selectedType == Post.PostType.TEXT,
                    onClick = { selectedType = Post.PostType.TEXT }
                )
                PostTypeChip(
                    label = "Workout",
                    icon = Icons.Default.FitnessCenter,
                    selected = selectedType == Post.PostType.WORKOUT,
                    onClick = { selectedType = Post.PostType.WORKOUT }
                )
                PostTypeChip(
                    label = "Progress",
                    icon = Icons.Default.ShowChart,
                    selected = selectedType == Post.PostType.PROGRESS_PHOTO,
                    onClick = { selectedType = Post.PostType.PROGRESS_PHOTO }
                )
            }

            Spacer(Modifier.height(20.dp))

            // Exercise name (Workout only)
            AnimatedVisibility(visible = selectedType == Post.PostType.WORKOUT) {
                Column {
                    OutlinedTextField(
                        value = exerciseType,
                        onValueChange = { exerciseType = it },
                        label = { Text("Exercise Name", color = TextSecondary) },
                        placeholder = { Text("e.g. Squats, Push-ups...", color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = postFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Content field
            OutlinedTextField(
                value = content,
                onValueChange = {
                    content = it
                    error = ""
                },
                label = {
                    Text(
                        when (selectedType) {
                            Post.PostType.WORKOUT -> "Workout Notes"
                            Post.PostType.PROGRESS_PHOTO -> "Progress Update"
                            else -> "What's on your mind?"
                        },
                        color = TextSecondary
                    )
                },
                placeholder = {
                    Text(
                        when (selectedType) {
                            Post.PostType.WORKOUT -> "Share your workout experience, PRs, tips..."
                            Post.PostType.PROGRESS_PHOTO -> "Describe your progress..."
                            else -> "Share something with the community..."
                        },
                        color = TextSecondary.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                colors = postFieldColors(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 12
            )

            if (error.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = RedDanger, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (content.isBlank()) {
                        error = "Content cannot be empty"
                    } else {
                        viewModel.createPost(
                            content = content,
                            type = selectedType,
                            exerciseType = exerciseType
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = DeepSpace
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share Post", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun PostTypeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) CyanPrimary.copy(alpha = 0.2f) else CardDark
    val tint = if (selected) CyanPrimary else TextSecondary
    val border = if (selected) 1.dp else 0.dp

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = if (selected) androidx.compose.foundation.BorderStroke(border, CyanPrimary.copy(alpha = 0.6f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 13.sp, color = tint, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun postFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = CardDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = CyanPrimary,
    focusedContainerColor = DarkNavy,
    unfocusedContainerColor = DarkNavy,
    focusedLabelColor = CyanPrimary
)
