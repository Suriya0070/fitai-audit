package com.jsf.app.pravin.ui.screens.livestream

import android.Manifest
import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.domain.ExerciseLibrary
import com.jsf.app.pravin.ui.theme.*
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveWorkoutScreen(
    serverId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val userId by prefs.currentUserId.collectAsStateWithLifecycle(initialValue = "")

    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )

    LaunchedEffect(Unit) {
        if (!permissions.allPermissionsGranted) {
            permissions.launchMultiplePermissionRequest()
        }
    }

    if (!permissions.allPermissionsGranted) {
        val permanentlyDenied = permissions.permissions.any {
            !it.status.isGranted && !it.status.shouldShowRationale
        }
        PermissionDeniedContent(permanentlyDenied = permanentlyDenied, onNavigateBack = onNavigateBack)
        return
    }

    val viewModel: LiveWorkoutViewModel = viewModel(
        factory = LiveWorkoutViewModel.factory(
            context.applicationContext as android.app.Application,
            serverId
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsStateWithLifecycle()
    val webRtcReady by viewModel.webRtcManagerReady.collectAsStateWithLifecycle()

    // Keep screen on during live workout
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Intercept hardware back button — end call cleanly before navigating
    BackHandler {
        viewModel.endCall()
        onNavigateBack()
    }

    var setupDone by remember { mutableStateOf(false) }
    LaunchedEffect(userId) {
        if (userId.isNotEmpty() && !setupDone) {
            setupDone = true
            viewModel.setup(userId)
        }
    }

    // Navigate back only when call status is ENDED
    LaunchedEffect(uiState.callStatus) {
        if (uiState.callStatus == CallStatus.ENDED) {
            onNavigateBack()
        }
    }

    val localRenderer = remember {
        runCatching {
            SurfaceViewRenderer(context).apply {
                setZOrderMediaOverlay(true)
                setMirror(true)
            }
        }.getOrNull()
    }
    val remoteRenderer = remember { runCatching { SurfaceViewRenderer(context) }.getOrNull() }
    var renderersInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(webRtcReady) {
        if (webRtcReady && !renderersInitialized && localRenderer != null && remoteRenderer != null) {
            val egl = viewModel.webRtcManager?.eglBase ?: return@LaunchedEffect
            runCatching {
                localRenderer.init(egl.eglBaseContext, null)
                localRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                remoteRenderer.init(egl.eglBaseContext, null)
                remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                viewModel.webRtcManager?.localVideoTrack?.addSink(localRenderer)
                renderersInitialized = true
            }.onFailure { it.printStackTrace() }
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            remoteVideoTrack?.removeSink(remoteRenderer)
            viewModel.webRtcManager?.localVideoTrack?.removeSink(localRenderer)
            runCatching { localRenderer?.release() }
            runCatching { remoteRenderer?.release() }
        }
    }

    LaunchedEffect(remoteVideoTrack, renderersInitialized) {
        if (renderersInitialized && remoteRenderer != null) {
            remoteVideoTrack?.addSink(remoteRenderer)
        }
    }

    // Update local preview mirror when camera flips
    LaunchedEffect(uiState.isFrontCamera) {
        if (renderersInitialized) {
            runCatching { localRenderer?.setMirror(uiState.isFrontCamera) }
        }
    }

    // Show init error as a full-screen overlay (not just a snackbar)
    if (uiState.isInitError) {
        InitErrorContent(
            message = uiState.errorMessage ?: "Video call unavailable on this device.",
            onNavigateBack = onNavigateBack
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Remote video — full background
        if (renderersInitialized && remoteRenderer != null) {
            AndroidView(
                factory = { remoteRenderer },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Waiting / Reconnecting overlay
        AnimatedVisibility(
            visible = uiState.callStatus != CallStatus.ACTIVE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (uiState.callStatus == CallStatus.RECONNECTING) {
                        Icon(
                            Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = OrangeWarning,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            color = CyanPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = when (uiState.callStatus) {
                            CallStatus.INITIALIZING -> "Starting camera..."
                            CallStatus.WAITING_FOR_PEER -> "Waiting for someone to join..."
                            CallStatus.CONNECTING -> "Connecting..."
                            CallStatus.RECONNECTING -> "Reconnecting..."
                            else -> ""
                        },
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (uiState.callStatus == CallStatus.RECONNECTING) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Check your network connection",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Local video PiP — top right
        if (renderersInitialized && localRenderer != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 8.dp)
            ) {
                AndroidView(
                    factory = { localRenderer },
                    modifier = Modifier
                        .size(110.dp, 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                // Camera off overlay on PiP
                if (!uiState.isCameraEnabled) {
                    Box(
                        modifier = Modifier
                            .size(110.dp, 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideocamOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.endCall(); onNavigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Leave", tint = Color.White)
            }

            // Call duration timer
            if (uiState.callStatus == CallStatus.ACTIVE || uiState.callDurationSeconds > 0) {
                CallDurationChip(uiState.callDurationSeconds)
            }

            Spacer(Modifier.weight(1f))

            // Connection quality badge
            ConnectionQualityBadge(uiState.callStatus)
            Spacer(Modifier.width(4.dp))

            // Share code (host only)
            if (uiState.isHost && uiState.callId.isNotEmpty()) {
                CallCodeChip(callId = uiState.callId)
            }
        }

        // Bottom controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 24.dp, top = 40.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Rep counter row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RepCountDisplay(
                        label = "You",
                        count = uiState.localRepCount,
                        color = CyanPrimary,
                        poseDetected = uiState.poseDetected
                    )
                    uiState.remoteRepCounts.entries.firstOrNull()?.let { (uid, count) ->
                        Spacer(Modifier.width(32.dp))
                        RepCountDisplay(
                            label = uid.take(6),
                            count = count,
                            color = NeonGreen,
                            poseDetected = true
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Exercise type chips — uses full library (popular exercises)
                val popularExercises = remember { ExerciseLibrary.getPopular() }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    popularExercises.forEach { exercise ->
                        val selected = uiState.exerciseType == exercise.name
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setExerciseType(exercise.name) },
                            label = { Text("${exercise.emoji} ${exercise.name}", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = DeepSpace,
                                containerColor = Color.Black.copy(alpha = 0.5f),
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Media control row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute
                    CallControlButton(
                        icon = if (uiState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (uiState.isMuted) "Unmute" else "Mute",
                        tint = if (uiState.isMuted) RedDanger else Color.White,
                        containerColor = Color.White.copy(alpha = 0.15f),
                        onClick = { viewModel.toggleMute() }
                    )

                    // End call — large center button
                    Button(
                        onClick = { viewModel.endCall(); onNavigateBack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedDanger,
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "End call",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Camera on/off
                    CallControlButton(
                        icon = if (uiState.isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = if (uiState.isCameraEnabled) "Camera" else "No Cam",
                        tint = if (!uiState.isCameraEnabled) RedDanger else Color.White,
                        containerColor = Color.White.copy(alpha = 0.15f),
                        onClick = { viewModel.toggleCamera() }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Camera flip button (separate row so it doesn't crowd the main controls)
                TextButton(
                    onClick = { viewModel.switchCamera() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                ) {
                    Icon(
                        Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip camera",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Flip Camera", fontSize = 12.sp)
                }
            }
        }

        // Error snackbar (dismissible)
        uiState.errorMessage?.let { msg ->
            if (!uiState.isInitError) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                    dismissAction = {
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("Dismiss", color = CyanPrimary)
                        }
                    }
                ) {
                    Text(msg, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CallDurationChip(seconds: Int) {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val text = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(RedDanger)
            )
            Spacer(Modifier.width(5.dp))
            Text(text, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ConnectionQualityBadge(status: CallStatus) {
    val (label, color) = when (status) {
        CallStatus.ACTIVE -> "Good" to Color(0xFF4ADE80)
        CallStatus.RECONNECTING -> "Poor" to OrangeWarning
        CallStatus.CONNECTING -> "..." to TextSecondary
        else -> return
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SignalCellularAlt,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier.size(52.dp)
        ) {
            IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun RepCountDisplay(
    label: String,
    count: Int,
    color: Color,
    poseDetected: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 52.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (poseDetected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun CallCodeChip(callId: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Text("Join Code", fontSize = 9.sp, color = TextSecondary)
                Text(callId, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
            }
        }
    }
}

@Composable
private fun InitErrorContent(message: String, onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.VideocamOff,
                contentDescription = null,
                tint = RedDanger,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Unable to Start Video Call",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Go Back", color = DeepSpace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(permanentlyDenied: Boolean, onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.VideocamOff,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Camera & Microphone Required",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (permanentlyDenied)
                    "Permissions were permanently denied. Please enable Camera and Microphone in your device Settings to use live workout."
                else
                    "Please grant Camera and Microphone permissions to use live workout.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Go Back", color = DeepSpace, fontWeight = FontWeight.Bold)
            }
        }
    }
}
