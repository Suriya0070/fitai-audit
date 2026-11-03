package com.jsf.app.pravin.ui.screens.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(
    channelId: String,
    channelName: String,
    onNavigateBack: () -> Unit,
    onNavigateToCreatePost: (channelId: String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: CommunityFeedViewModel = viewModel(
        factory = CommunityFeedViewModel.factory(app, channelId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "#$channelName",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToCreatePost(channelId) },
                containerColor = CyanPrimary,
                contentColor = DeepSpace
            ) {
                Icon(Icons.Default.Create, contentDescription = "Create Post")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
            return@Scaffold
        }

        if (state.posts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DynamicFeed,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No posts yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Share something with the community!",
                        fontSize = 14.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    reactions = state.reactionsMap[post.id] ?: emptyList(),
                    comments = state.commentsMap[post.id] ?: emptyList(),
                    currentUserId = state.currentUserId,
                    onToggleReaction = { emoji -> viewModel.toggleReaction(post.id, emoji) },
                    onAddComment = { content -> viewModel.addComment(post.id, content) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
