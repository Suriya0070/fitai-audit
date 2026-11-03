package com.jsf.app.pravin.ui.screens.friends

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.remote.FriendRequest
import com.jsf.app.pravin.data.remote.Friendship
import com.jsf.app.pravin.data.remote.PublicUserProfile
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FriendsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Friends (${state.friends.size})",
        "Add",
        if (state.receivedRequests.isNotEmpty()) "Requests (${state.receivedRequests.size})" else "Requests"
    )

    LaunchedEffect(state.snackMessage) {
        state.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnack()
        }
    }

    Scaffold(
        containerColor = LightBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Friends", fontWeight = FontWeight.ExtraBold, color = TextDark, fontSize = 20.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightCard)
            )
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
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> FriendsListTab(
                    friends = state.friends,
                    onRemove = { viewModel.removeFriend(it) }
                )
                1 -> AddFriendsTab(
                    searchQuery = state.searchQuery,
                    searchResults = state.searchResults,
                    isSearching = state.isSearching,
                    sentRequestIds = state.sentRequestIds,
                    currentFriendIds = state.friends.flatMap { listOf(it.userId1, it.userId2) }.toSet(),
                    currentUserId = state.currentUserId,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onSendRequest = { viewModel.sendRequest(it) }
                )
                2 -> RequestsTab(
                    requests = state.receivedRequests,
                    onAccept = { viewModel.acceptRequest(it) },
                    onDecline = { viewModel.declineRequest(it) }
                )
            }
        }
    }
}

@Composable
private fun FriendsListTab(
    friends: List<Friendship>,
    onRemove: (Friendship) -> Unit
) {
    if (friends.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Group, null, tint = TextGray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("No friends yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(6.dp))
                Text("Search for people in the Add tab", fontSize = 14.sp, color = TextGray)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(friends, key = { it.id }) { friendship ->
            FriendCard(friendship = friendship, onRemove = { onRemove(friendship) })
        }
    }
}

@Composable
private fun FriendCard(friendship: Friendship, onRemove: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(name = friendship.friendName, size = 46)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friendship.friendName.ifEmpty { "Unknown" },
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark
                )
                if (friendship.friendFitnessLevel.isNotEmpty()) {
                    Text(friendship.friendFitnessLevel, fontSize = 12.sp, color = TextGray)
                }
            }
            if (showConfirm) {
                TextButton(onClick = {
                    onRemove()
                    showConfirm = false
                }) {
                    Text("Remove", color = RedDanger, fontSize = 13.sp)
                }
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = TextGray, fontSize = 13.sp)
                }
            } else {
                IconButton(onClick = { showConfirm = true }) {
                    Icon(Icons.Default.PersonRemove, null, tint = TextGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFriendsTab(
    searchQuery: String,
    searchResults: List<PublicUserProfile>,
    isSearching: Boolean,
    sentRequestIds: Set<String>,
    currentFriendIds: Set<String>,
    currentUserId: String,
    onQueryChange: (String) -> Unit,
    onSendRequest: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search by name...", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextGray) },
            trailingIcon = {
                if (isSearching) CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CyanPrimary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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

        if (searchQuery.length < 2) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PersonSearch, null, tint = TextGray.copy(0.4f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Type at least 2 characters to search", fontSize = 14.sp, color = TextGray)
                }
            }
            return
        }

        if (searchResults.isEmpty() && !isSearching) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users found for \"$searchQuery\"", fontSize = 14.sp, color = TextGray)
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(searchResults, key = { it.userId }) { user ->
                val isAlreadyFriend = currentFriendIds.contains(user.userId)
                val requestSent = sentRequestIds.contains(user.userId)
                SearchResultCard(
                    user = user,
                    isAlreadyFriend = isAlreadyFriend,
                    requestSent = requestSent,
                    onSendRequest = { onSendRequest(user.userId) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    user: PublicUserProfile,
    isAlreadyFriend: Boolean,
    requestSent: Boolean,
    onSendRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(name = user.name, size = 46)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                if (user.fitnessLevel.isNotEmpty()) {
                    Text(user.fitnessLevel, fontSize = 12.sp, color = TextGray)
                }
            }
            when {
                isAlreadyFriend -> Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GreenSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        "Friends",
                        fontSize = 12.sp, color = GreenSuccess, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                requestSent -> Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyanPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        "Sent",
                        fontSize = 12.sp, color = CyanPrimary, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                else -> Button(
                    onClick = onSendRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RequestsTab(
    requests: List<FriendRequest>,
    onAccept: (FriendRequest) -> Unit,
    onDecline: (FriendRequest) -> Unit
) {
    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Inbox, null, tint = TextGray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("No pending requests", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            RequestCard(
                request = request,
                onAccept = { onAccept(request) },
                onDecline = { onDecline(request) }
            )
        }
    }
}

@Composable
private fun RequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(name = request.fromName, size = 46)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.fromName.ifEmpty { "Someone" },
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark
                )
                Text("Wants to be your friend", fontSize = 12.sp, color = TextGray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RedDanger.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Default.Close, null, tint = RedDanger, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GreenSuccess.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Default.Check, null, tint = GreenSuccess, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(name: String, size: Int) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(CyanPrimary.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            fontSize = (size * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            color = CyanPrimary
        )
    }
}
