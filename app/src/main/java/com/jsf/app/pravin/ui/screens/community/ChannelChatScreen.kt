package com.jsf.app.pravin.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.model.Channel
import com.jsf.app.pravin.data.model.Message
import com.jsf.app.pravin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChannelChatScreen(
    channelId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: ChannelChatViewModel = viewModel(
        factory = ChannelChatViewModel.factory(app, channelId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val channel = state.channel
    val isAnnouncements = channel?.type == Channel.ChannelType.ANNOUNCEMENTS
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channelPrefix(channel?.type ?: "TEXT"),
                            fontSize = 18.sp,
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = channel?.name ?: "Loading...",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        },
        bottomBar = {
            if (isAnnouncements) {
                Surface(color = DarkNavy) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = OrangeWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Announcements channel — read only",
                            fontSize = 13.sp,
                            color = OrangeWarning
                        )
                    }
                }
            } else {
                Surface(color = DarkNavy) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Message #${channel?.name ?: "..."}", color = TextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = CardDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = CyanPrimary,
                                focusedContainerColor = DeepSpace,
                                unfocusedContainerColor = DeepSpace
                            ),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = false,
                            maxLines = 4
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            },
                            enabled = messageText.isNotBlank(),
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (messageText.isNotBlank()) CyanPrimary else CardDark,
                                    RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Send",
                                tint = if (messageText.isNotBlank()) DeepSpace else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
            return@Scaffold
        }

        if (state.messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Forum,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No messages yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Start the conversation!",
                        fontSize = 13.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.messages, key = { it.id }) { message ->
                val isOwn = message.authorId == state.currentUserId
                var showMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    when {
                        message.attachmentType == Message.AttachmentType.WORKOUT_RESULT -> {
                            WorkoutShareBubble(
                                message = message,
                                isOwn = isOwn,
                                onLongPress = { if (isOwn) showMenu = true }
                            )
                        }
                        else -> {
                            MessageBubble(
                                message = message,
                                isOwn = isOwn,
                                displayName = if (isOwn) state.currentUserName else message.authorId.take(8),
                                onLongPress = { if (isOwn) showMenu = true }
                            )
                        }
                    }
                    if (showMenu && isOwn) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { showMenu = false },
                            containerColor = CardDark
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = RedDanger) },
                                onClick = {
                                    viewModel.deleteMessage(message.id)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = RedDanger)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    displayName: String,
    onLongPress: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val bubbleColor = if (isOwn) CyanPrimary.copy(alpha = 0.15f) else CardDark
    val nameColor = if (isOwn) NeonGreen else CyanPrimary

    Column(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .combinedClickable(onLongClick = onLongPress, onClick = {}),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
        ) {
            Text(displayName, fontSize = 11.sp, color = nameColor, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Text(
                timeFormat.format(Date(message.timestamp)),
                fontSize = 10.sp,
                color = TextSecondary.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(2.dp))
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isOwn) 16.dp else 4.dp,
                topEnd = if (isOwn) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = bubbleColor
        ) {
            Text(
                text = message.content,
                color = TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WorkoutShareBubble(
    message: Message,
    isOwn: Boolean,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(onLongClick = onLongPress, onClick = {}),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏋️", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Workout Share", fontSize = 11.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                Text(message.content, fontSize = 13.sp, color = TextPrimary)
            }
        }
    }
}

private fun channelPrefix(type: String): String = when (type) {
    Channel.ChannelType.TEXT -> "#"
    Channel.ChannelType.ANNOUNCEMENTS -> "📢"
    Channel.ChannelType.WORKOUT -> "🏋️"
    Channel.ChannelType.NUTRITION -> "🍎"
    Channel.ChannelType.LIVE -> "🔴"
    Channel.ChannelType.CHALLENGES -> "🏆"
    Channel.ChannelType.PROGRESS -> "📈"
    Channel.ChannelType.LEADERBOARD -> "🥇"
    else -> "#"
}
