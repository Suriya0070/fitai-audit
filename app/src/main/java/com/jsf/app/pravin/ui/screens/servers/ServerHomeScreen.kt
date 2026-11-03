package com.jsf.app.pravin.ui.screens.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.model.Channel
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerHomeScreen(
    serverId: String,
    onNavigateBack: () -> Unit,
    onStartWorkout: () -> Unit = {},
    onNavigateToChannel: (channelId: String, channelType: String) -> Unit = { _, _ -> },
    onNavigateToRoutines: (serverId: String) -> Unit = {},
    onNavigateToMembers: (serverId: String) -> Unit = {},
    onNavigateToLive: (serverId: String) -> Unit = {},
    onNavigateToRepBattle: (serverId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ServerHomeViewModel = viewModel(
        factory = ServerHomeViewModel.factory(context.applicationContext as android.app.Application, serverId)
    )

    val server by viewModel.server.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()

    var selectedChannelId by remember { mutableStateOf<String?>(null) }

    val selectedChannel = channels.find { it.id == selectedChannelId }
        ?: channels.firstOrNull()

    LaunchedEffect(channels) {
        if (selectedChannelId == null && channels.isNotEmpty()) {
            selectedChannelId = channels.first().id
        }
    }

    val accentColor = server?.accentColor?.let { hex ->
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(CyanPrimary)
    } ?: CyanPrimary

    Scaffold(
        containerColor = LightBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = server?.name ?: "Loading...",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark,
                            fontSize = 18.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GreenSuccess)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${members.size} members",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToLive(serverId) }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Go Live", tint = RedDanger)
                    }
                    IconButton(onClick = { onNavigateToRoutines(serverId) }) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = "Routines", tint = TextGray)
                    }
                    IconButton(onClick = { onNavigateToMembers(serverId) }) {
                        Icon(Icons.Default.People, contentDescription = "Members", tint = TextGray)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightCard)
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Channel sidebar
            Surface(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight(),
                color = LightSurface
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (channels.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No channels", fontSize = 13.sp, color = TextGray)
                            }
                        }
                    } else {
                        items(channels, key = { it.id }) { channel ->
                            ChannelItem(
                                channel = channel,
                                isSelected = channel.id == (selectedChannel?.id),
                                accentColor = accentColor,
                                onClick = {
                                    selectedChannelId = channel.id
                                    onNavigateToChannel(channel.id, channel.type)
                                }
                            )
                        }
                    }
                }
            }

            VerticalDivider(color = LightBorder, thickness = 1.dp)

            // Main content area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightBg)
            ) {
                // Channel header
                selectedChannel?.let { ch ->
                    Surface(color = LightBg) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = channelIcon(ch.type),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = ch.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            if (ch.description.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "— ${ch.description}",
                                    fontSize = 13.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = LightBorder)
                }

                // Contextual action buttons
                selectedChannel?.let { ch ->
                    when (ch.type) {
                        Channel.ChannelType.WORKOUT -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Button(
                                    onClick = onStartWorkout,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreen,
                                        contentColor = DeepSpace
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Start Workout", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Channel.ChannelType.LIVE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToLive(serverId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RedDanger,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Live Session", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Channel.ChannelType.CHALLENGES -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToRepBattle(serverId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ScoreGold,
                                        contentColor = DeepSpace
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Rep Battles 🏆", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        else -> {}
                    }
                }

                // Channel content placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = selectedChannel?.let { channelIcon(it.type) }
                                ?: Icons.Default.Forum,
                            contentDescription = null,
                            tint = TextGray.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Welcome to #${selectedChannel?.name ?: "general"}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "This is the beginning of this channel.\nChat & content coming in the next phase.",
                            fontSize = 13.sp,
                            color = TextGray.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelItem(
    channel: Channel,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bg = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (isSelected) accentColor else TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = channelIcon(channel.type),
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = channel.name,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

private fun channelIcon(type: String): ImageVector = when (type) {
    Channel.ChannelType.TEXT -> Icons.Default.Tag
    Channel.ChannelType.WORKOUT -> Icons.Default.FitnessCenter
    Channel.ChannelType.NUTRITION -> Icons.Default.Restaurant
    Channel.ChannelType.ANNOUNCEMENTS -> Icons.Default.Campaign
    Channel.ChannelType.LIVE -> Icons.Default.LiveTv
    Channel.ChannelType.CHALLENGES -> Icons.Default.EmojiEvents
    Channel.ChannelType.PROGRESS -> Icons.Default.ShowChart
    Channel.ChannelType.LEADERBOARD -> Icons.Default.Leaderboard
    else -> Icons.Default.Tag
}
