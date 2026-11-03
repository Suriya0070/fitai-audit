package com.jsf.app.pravin.ui.screens.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.model.Server
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    onNavigateToServer: (String) -> Unit,
    onNavigateToCreateServer: () -> Unit,
    viewModel: ServerListViewModel = viewModel()
) {
    val myServers by viewModel.myServers.collectAsStateWithLifecycle()
    val discoverServers by viewModel.discoverServers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("My Servers", "Discover")

    Scaffold(
        containerColor = LightBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Community",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightCard)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateServer,
                containerColor = CyanPrimary,
                contentColor = DeepSpace
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Server")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = LightCard,
                contentColor = CyanPrimary
            ) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Text(
                                label,
                                color = if (selectedTab == idx) CyanPrimary else TextSecondary,
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> MyServersTab(
                    servers = myServers,
                    onServerClick = onNavigateToServer,
                    onCreateServer = onNavigateToCreateServer
                )
                1 -> DiscoverTab(
                    servers = discoverServers,
                    searchQuery = searchQuery,
                    onSearchChange = viewModel::setSearchQuery,
                    onServerClick = onNavigateToServer,
                    onJoinServer = viewModel::joinServer
                )
            }
        }
    }
}

@Composable
private fun MyServersTab(
    servers: List<Server>,
    onServerClick: (String) -> Unit,
    onCreateServer: () -> Unit
) {
    if (servers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No servers yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Create or join a community to get started",
                    fontSize = 14.sp,
                    color = TextGray
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onCreateServer,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Create a Server", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(servers, key = { it.id }) { server ->
                ServerCard(server = server, onClick = { onServerClick(server.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverTab(
    servers: List<Server>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onServerClick: (String) -> Unit,
    onJoinServer: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search servers...", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = LightBorder,
                focusedTextColor = TextDark,
                unfocusedTextColor = TextDark,
                cursorColor = CyanPrimary,
                focusedContainerColor = LightCard,
                unfocusedContainerColor = LightCard
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(servers, key = { it.id }) { server ->
                ServerCard(
                    server = server,
                    onClick = { onServerClick(server.id) },
                    trailingAction = {
                        OutlinedButton(
                            onClick = { onJoinServer(server.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Join", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ServerCard(
    server: Server,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(server.accentColor)) }
        .getOrDefault(CyanPrimary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ServerAvatar(name = server.name, accentColor = accentColor, size = 52)

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (server.description.isNotBlank()) {
                    Text(
                        text = server.description,
                        fontSize = 13.sp,
                        color = TextGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GreenSuccess)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${server.memberCount} members",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                    if (!server.isPublic) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = OrangeWarning.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Private",
                                fontSize = 11.sp,
                                color = OrangeWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            trailingAction?.invoke()
        }
    }
}

@Composable
fun ServerAvatar(name: String, accentColor: Color, size: Int) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            fontSize = (size * 0.42f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor
        )
    }
}

