package com.jsf.app.pravin.ui.screens.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServerScreen(
    onNavigateBack: () -> Unit,
    onServerCreated: (String) -> Unit,
    viewModel: CreateServerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var serverName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf("#00D4FF") }

    val accentColors = listOf(
        "#00D4FF" to CyanPrimary,
        "#39FF14" to NeonGreen,
        "#FFD700" to ScoreGold,
        "#FF8C00" to OrangeWarning,
        "#FF3131" to RedDanger,
        "#9B59B6" to Color(0xFF9B59B6)
    )

    LaunchedEffect(uiState.createdServerId) {
        uiState.createdServerId?.let { serverId ->
            onServerCreated(serverId)
        }
    }

    LaunchedEffect(uiState.error) {
        // Error shown inline
    }

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Text("Create Server", fontWeight = FontWeight.Bold, color = TextPrimary)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live preview card
            val previewAccent = accentColors.find { it.first == selectedColor }?.second ?: CyanPrimary
            Text("Preview", fontSize = 13.sp, color = TextSecondary)
            ServerCard(
                server = com.jsf.app.pravin.data.model.Server(
                    id = "preview",
                    name = serverName.ifBlank { "Server Name" },
                    description = description.ifBlank { "Your server description" },
                    ownerId = "",
                    accentColor = selectedColor,
                    isPublic = isPublic
                ),
                onClick = {}
            )

            HorizontalDivider(color = CardDark)

            // Server name
            OutlinedTextField(
                value = serverName,
                onValueChange = { serverName = it },
                label = { Text("Server Name *", color = TextSecondary) },
                placeholder = { Text("e.g. Morning Warriors", color = TextSecondary.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = CardDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanPrimary,
                    focusedContainerColor = DarkNavy,
                    unfocusedContainerColor = DarkNavy
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description", color = TextSecondary) },
                placeholder = { Text("What is this server about?", color = TextSecondary.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = CardDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanPrimary,
                    focusedContainerColor = DarkNavy,
                    unfocusedContainerColor = DarkNavy
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            // Public / Private toggle
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPublic) "Public Server" else "Private Server",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isPublic) "Anyone can discover and join" else "Invite only",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = CyanPrimary,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkNavy
                        )
                    )
                }
            }

            // Accent color picker
            Column {
                Text("Accent Color", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    accentColors.forEach { (hex, color) ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, TextPrimary, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Error message
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = RedDanger,
                    fontSize = 13.sp
                )
            }

            // Create button
            Button(
                onClick = {
                    viewModel.createServer(
                        name = serverName,
                        description = description,
                        isPublic = isPublic,
                        accentColor = selectedColor
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = serverName.isNotBlank() && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = DeepSpace,
                    disabledContainerColor = CyanPrimary.copy(alpha = 0.4f),
                    disabledContentColor = DeepSpace.copy(alpha = 0.5f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = DeepSpace,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text("Create Server", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
