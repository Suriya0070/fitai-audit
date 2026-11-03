package com.jsf.app.pravin.ui.screens.trainer

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.remote.TrainerClient
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerDashboardScreen(
    onNavigateToClientDetail: (String) -> Unit,
    onNavigateToInvite: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val vm: TrainerDashboardViewModel = viewModel()
    val clients by vm.clients.collectAsStateWithLifecycle()
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    val unreadCount by vm.unreadAlertCount.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Clients", "Alerts")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trainer Dashboard", fontWeight = FontWeight.Bold, color = TextDark) },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = TextGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightCard)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onNavigateToInvite,
                    containerColor = CyanPrimary,
                    contentColor = DeepSpace
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Invite client")
                }
            }
        },
        containerColor = LightBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(
                    label = "Clients",
                    value = "${clients.size}",
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Alerts",
                    value = "$unreadCount",
                    icon = Icons.Default.Notifications,
                    highlight = unreadCount > 0,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "At Risk",
                    value = "${clients.count { it.dropoutRisk > 0.6f }}",
                    icon = Icons.Default.Warning,
                    highlight = clients.any { it.dropoutRisk > 0.6f },
                    modifier = Modifier.weight(1f)
                )
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = LightCard,
                contentColor = CyanPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(title)
                                if (index == 1 && unreadCount > 0) {
                                    Badge { Text("$unreadCount") }
                                }
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> ClientsTab(clients = clients, onClientClick = onNavigateToClientDetail)
                1 -> AlertsTab(alerts = alerts, onMarkRead = { vm.markAlertRead(it) })
            }
        }
    }
}

@Composable
private fun ClientsTab(
    clients: List<TrainerClient>,
    onClientClick: (String) -> Unit
) {
    if (clients.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Group, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("No clients yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextGray)
                Text("Tap + to invite a client", fontSize = 14.sp, color = TextGray)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(clients, key = { it.id }) { client ->
            ClientCard(client = client, onClick = { onClientClick(client.clientId) })
        }
    }
}

@Composable
private fun ClientCard(client: TrainerClient, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar initial
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CyanPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = client.clientName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(client.clientName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextDark)
                Text(client.clientEmail, fontSize = 12.sp, color = TextGray)
                if (client.overallScore > 0) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { client.overallScore / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = scoreColor(client.overallScore),
                        trackColor = LightBorder
                    )
                    Text("Score: ${client.overallScore.toInt()}%", fontSize = 11.sp, color = TextGray)
                }
            }

            if (client.dropoutRisk > 0.5f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "At risk", tint = OrangeWarning, modifier = Modifier.size(20.dp))
                    Text(
                        "${(client.dropoutRisk * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = OrangeWarning,
                        fontWeight = FontWeight.Bold
                    )
                    Text("risk", fontSize = 10.sp, color = OrangeWarning)
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray)
        }
    }
}

@Composable
private fun AlertsTab(
    alerts: List<com.jsf.app.pravin.data.remote.TrainerAlert>,
    onMarkRead: (String) -> Unit
) {
    if (alerts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("No alerts", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextGray)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(alerts, key = { it.id }) { alert ->
            AlertItem(alert = alert, onMarkRead = { onMarkRead(alert.id) })
        }
    }
}

@Composable
internal fun AlertItem(
    alert: com.jsf.app.pravin.data.remote.TrainerAlert,
    onMarkRead: () -> Unit
) {
    val bgColor = if (alert.isRead) LightCard else CyanPrimary.copy(alpha = 0.06f)
    val iconTint = when (alert.type) {
        com.jsf.app.pravin.data.remote.TrainerAlert.Type.MISSED_WORKOUT -> OrangeWarning
        com.jsf.app.pravin.data.remote.TrainerAlert.Type.FORM_REGRESSION -> RedDanger
        com.jsf.app.pravin.data.remote.TrainerAlert.Type.DROPOUT_RISK -> RedDanger
        else -> TextGray
    }
    val alertIcon = when (alert.type) {
        com.jsf.app.pravin.data.remote.TrainerAlert.Type.MISSED_WORKOUT -> Icons.Default.EventBusy
        com.jsf.app.pravin.data.remote.TrainerAlert.Type.FORM_REGRESSION -> Icons.Default.TrendingDown
        else -> Icons.Default.Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        onClick = { if (!alert.isRead) onMarkRead() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(alertIcon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.message, fontSize = 14.sp, color = TextDark, fontWeight = if (!alert.isRead) FontWeight.SemiBold else FontWeight.Normal)
                Text(
                    formatRelativeTime(alert.createdAt),
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
            if (!alert.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary)
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) OrangeWarning.copy(alpha = 0.1f) else LightCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null,
                tint = if (highlight) OrangeWarning else CyanPrimary,
                modifier = Modifier.size(20.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = if (highlight) OrangeWarning else TextDark)
            Text(label, fontSize = 11.sp, color = TextGray)
        }
    }
}

private fun scoreColor(score: Float): Color = when {
    score >= 75 -> Color(0xFF16A34A)
    score >= 50 -> Color(0xFFF59E0B)
    else -> Color(0xFFEF4444)
}

private fun formatRelativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val mins = diff / (1000 * 60)
    val hours = mins / 60
    val days = hours / 24
    return when {
        mins < 2 -> "Just now"
        mins < 60 -> "$mins min ago"
        hours < 24 -> "$hours hr ago"
        days == 1L -> "Yesterday"
        else -> "$days days ago"
    }
}
