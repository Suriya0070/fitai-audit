package com.jsf.app.pravin.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.ui.theme.*

@Composable
fun AuthScreen(
    onAuthSuccess: (uid: String, isNewUser: Boolean) -> Unit
) {
    val viewModel: AuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    // Navigate on success
    LaunchedEffect(uiState.successUid) {
        uiState.successUid?.let { uid ->
            onAuthSuccess(uid, uiState.isNewUser)
        }
    }

    // Password reset success dialog
    if (uiState.passwordResetSent) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPasswordResetFlag() },
            title = { Text("Email Sent", color = TextDark) },
            text = { Text("Password reset instructions sent to $resetEmail", color = TextGray) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPasswordResetFlag() }) {
                    Text("OK", color = CyanPrimary)
                }
            },
            containerColor = LightCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Forgot password dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Password", color = TextDark, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your email and we'll send reset instructions.", color = TextGray, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = LightBorder,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            cursorColor = CyanPrimary,
                            focusedContainerColor = LightCard,
                            unfocusedContainerColor = LightCard
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendPasswordReset(resetEmail)
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
                ) { Text("Send", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            },
            containerColor = LightCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // ── Branding ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(CyanPrimary, NeonGreen))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("FA", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DeepSpace)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "FIT AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDark,
                letterSpacing = 4.sp
            )
            Text(
                text = "Community Fitness Platform",
                fontSize = 13.sp,
                color = TextGray,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(36.dp))

            // ── Mode tabs ────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = LightSurface
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    AuthModeTab(
                        text = "Sign In",
                        selected = mode == AuthMode.SIGN_IN,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            mode = AuthMode.SIGN_IN
                            viewModel.clearError()
                        }
                    )
                    AuthModeTab(
                        text = "Create Account",
                        selected = mode == AuthMode.SIGN_UP,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            mode = AuthMode.SIGN_UP
                            viewModel.clearError()
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Email field ──────────────────────────────────────────────────
            AuthTextField(
                value = email,
                onValueChange = { email = it; viewModel.clearError() },
                label = "Email",
                leadingIcon = { Icon(Icons.Default.Email, null, tint = TextGray) },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Spacer(Modifier.height(12.dp))

            // ── Password field ───────────────────────────────────────────────
            AuthTextField(
                value = password,
                onValueChange = { password = it; viewModel.clearError() },
                label = "Password",
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextGray) },
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextGray
                        )
                    }
                },
                imeAction = if (mode == AuthMode.SIGN_UP) ImeAction.Next else ImeAction.Done,
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = {
                    focusManager.clearFocus()
                    if (mode == AuthMode.SIGN_IN) viewModel.signIn(email, password)
                }
            )

            // ── Confirm password (sign up only) ──────────────────────────────
            AnimatedVisibility(
                visible = mode == AuthMode.SIGN_UP,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; viewModel.clearError() },
                        label = "Confirm Password",
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextGray) },
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        imeAction = ImeAction.Done,
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.signUp(email, password, confirmPassword)
                        }
                    )
                }
            }

            // ── Forgot password ──────────────────────────────────────────────
            AnimatedVisibility(visible = mode == AuthMode.SIGN_IN) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { resetEmail = email; showResetDialog = true }) {
                        Text("Forgot password?", color = CyanPrimary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Error message ────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = RedDanger.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = uiState.error ?: "",
                        color = RedDanger,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ── Primary action button ────────────────────────────────────────
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (mode == AuthMode.SIGN_IN) viewModel.signIn(email, password)
                    else viewModel.signUp(email, password, confirmPassword)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = DeepSpace,
                    disabledContainerColor = CyanPrimary.copy(alpha = 0.4f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = DeepSpace,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (mode == AuthMode.SIGN_IN) "Sign In" else "Create Account",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Divider ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = LightBorder)
                Text(
                    "  or  ",
                    color = TextGray,
                    fontSize = 12.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = LightBorder)
            }

            Spacer(Modifier.height(16.dp))

            // ── Google button (scaffold — requires Google Sign-In setup) ─────
            OutlinedButton(
                onClick = { /* Phase 3: Google Sign-In */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder),
                enabled = false // Requires Google Sign-In configuration
            ) {
                Text("G", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TextGray)
                Spacer(Modifier.width(10.dp))
                Text("Continue with Google", color = TextGray, fontSize = 15.sp)
            }

            Spacer(Modifier.height(12.dp))

            // ── Local / guest mode — always available ─────────────────────────
            TextButton(
                onClick = { viewModel.continueAsLocalUser() },
                enabled = !uiState.isLoading
            ) {
                Text(
                    "Explore without an account",
                    color = TextGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Terms ────────────────────────────────────────────────────────
            AnimatedVisibility(visible = mode == AuthMode.SIGN_UP) {
                Text(
                    text = "By creating an account you agree to our Terms of Service and Privacy Policy",
                    color = TextGray.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AuthModeTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) CyanPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) DeepSpace else TextSecondary
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextGray, fontSize = 13.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onNext?.invoke() },
            onDone = { onDone?.invoke() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = LightBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = CyanPrimary,
            focusedContainerColor = LightCard,
            unfocusedContainerColor = LightCard,
            focusedLabelColor = CyanPrimary
        ),
        shape = RoundedCornerShape(14.dp)
    )
}
