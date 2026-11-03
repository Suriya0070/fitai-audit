package com.jsf.app.pravin.ui.screens.servers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.model.ServerMember
import com.jsf.app.pravin.data.model.ServerRole
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerMembersScreen(
    serverId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: ServerMembersViewModel = viewModel(
        factory = ServerMembersViewModel.factory(app, serverId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var roleChangeTarget by remember { mutableStateOf<ServerMember?>(null) }

    val roleOrder = listOf(
        ServerRole.OWNER,
        ServerRole.ADMIN,
        ServerRole.MODERATOR,
        ServerRole.COACH,
        ServerRole.MEMBER
    )

    val grouped = state.members
        .sortedWith(compareBy { roleOrder.indexOf(it.role).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE })
        .groupBy { it.role }

    val canManage = state.currentUserRole == ServerRole.OWNER || state.currentUserRole == ServerRole.ADMIN

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Members", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("${state.members.size} members", fontSize = 12.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            roleOrder.forEach { role ->
                val membersInRole = grouped[role] ?: return@forEach
                stickyHeader {
                    Surface(color = DeepSpace) {
                        Text(
                            text = "${roleLabel(role)} — ${membersInRole.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = roleColor(role),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
                items(membersInRole, key = { it.id }) { member ->
                    MemberRow(
                        member = member,
                        canManage = canManage && member.role != ServerRole.OWNER,
                        onChangeRole = { roleChangeTarget = member },
                        onRemove = { viewModel.removeMember(member) }
                    )
                }
            }
        }
    }

    roleChangeTarget?.let { target ->
        RoleChangeDialog(
            member = target,
            onDismiss = { roleChangeTarget = null },
            onConfirm = { newRole ->
                viewModel.updateMemberRole(target, newRole)
                roleChangeTarget = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemberRow(
    member: ServerMember,
    canManage: Boolean,
    onChangeRole: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { if (canManage) showMenu = true }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Initials avatar
        val initial = (member.nickname.firstOrNull() ?: member.userId.firstOrNull())
            ?.uppercaseChar()?.toString() ?: "?"
        val avatarColor = memberAvatarColor(member.userId)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = avatarColor)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val displayName = member.nickname.ifBlank { member.userId.take(12) }
            Text(
                displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Joined ${formatJoinDate(member.joinedAt)}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = roleColor(member.role).copy(alpha = 0.18f)
        ) {
            Text(
                roleLabel(member.role),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = roleColor(member.role),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        if (canManage) {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = CardDark
                ) {
                    DropdownMenuItem(
                        text = { Text("Change Role", color = TextPrimary) },
                        onClick = { showMenu = false; onChangeRole() },
                        leadingIcon = { Icon(Icons.Default.ManageAccounts, null, tint = CyanPrimary) }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove", color = RedDanger) },
                        onClick = { showMenu = false; onRemove() },
                        leadingIcon = { Icon(Icons.Default.PersonRemove, null, tint = RedDanger) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleChangeDialog(
    member: ServerMember,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val roles = listOf(ServerRole.ADMIN, ServerRole.MODERATOR, ServerRole.COACH, ServerRole.MEMBER)
    var selected by remember { mutableStateOf(member.role) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("Change Role", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select a new role:", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                roles.forEach { role ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == role,
                            onClick = { selected = role },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary, unselectedColor = TextSecondary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(roleLabel(role), color = TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

private fun roleLabel(role: String) = when (role) {
    ServerRole.OWNER -> "Owner"
    ServerRole.ADMIN -> "Admin"
    ServerRole.MODERATOR -> "Moderator"
    ServerRole.COACH -> "Coach"
    else -> "Member"
}

private fun roleColor(role: String): Color = when (role) {
    ServerRole.OWNER -> ScoreGold
    ServerRole.ADMIN -> RedDanger
    ServerRole.MODERATOR -> OrangeWarning
    ServerRole.COACH -> NeonGreen
    else -> CyanPrimary
}

private fun memberAvatarColor(userId: String): Color {
    val colors = listOf(CyanPrimary, NeonGreen, OrangeWarning, ScoreGold, RedDanger)
    return colors[Math.abs(userId.hashCode()) % colors.size]
}

private fun formatJoinDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
