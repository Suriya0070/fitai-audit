package com.jsf.app.pravin.ui.screens.trainer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientInviteScreen(onNavigateBack: () -> Unit) {
    val vm: TrainerDashboardViewModel = viewModel()
    val inviteResult by vm.inviteResult.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var trainerName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite Client", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightCard)
            )
        },
        containerColor = LightBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (val result = inviteResult) {
                is InviteResult.Success -> {
                    SuccessState(email = result.email, onDone = {
                        vm.clearInviteResult()
                        onNavigateBack()
                    })
                }
                else -> {
                    InviteForm(
                        email = email,
                        onEmailChange = { email = it },
                        trainerName = trainerName,
                        onNameChange = { trainerName = it },
                        error = (result as? InviteResult.Error)?.message,
                        onSend = { vm.inviteClient(email, trainerName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteForm(
    email: String,
    onEmailChange: (String) -> Unit,
    trainerName: String,
    onNameChange: (String) -> Unit,
    error: String?,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
    Spacer(Modifier.height(16.dp))

    Icon(
        Icons.Default.Email,
        contentDescription = null,
        tint = CyanPrimary,
        modifier = Modifier.size(64.dp)
    )

    Text(
        "Invite a Client",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        textAlign = TextAlign.Center
    )
    Text(
        "Enter your client's email address. They'll receive an invitation to connect with you on FitAI.",
        fontSize = 14.sp,
        color = TextGray,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = trainerName,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Your Name", color = TextGray) },
        placeholder = { Text("How you'll appear to the client", color = TextGray.copy(alpha = 0.5f)) },
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

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Client Email", color = TextGray) },
        placeholder = { Text("client@example.com", color = TextGray.copy(alpha = 0.5f)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        isError = error != null,
        supportingText = error?.let { { Text(it, color = RedDanger) } },
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

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = onSend,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = email.contains("@") && email.contains(".") && trainerName.isNotBlank(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
    ) {
        Text("Send Invitation", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
    } // end Column
}

@Composable
private fun SuccessState(email: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(80.dp))
        Text("Invitation Sent!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(
            "An invitation has been sent to\n$email",
            fontSize = 15.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
        Text(
            "Your client will appear in your dashboard once they accept.",
            fontSize = 13.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
        ) {
            Text("Done", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
