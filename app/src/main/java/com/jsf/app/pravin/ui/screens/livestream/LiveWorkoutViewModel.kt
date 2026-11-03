package com.jsf.app.pravin.ui.screens.livestream

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.remote.IceCandidateData
import com.jsf.app.pravin.data.remote.WebRtcSignalingService
import com.jsf.app.pravin.domain.PoseAnalyzer
import com.jsf.app.pravin.domain.RepCounter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.VideoTrack

enum class CallStatus { INITIALIZING, WAITING_FOR_PEER, CONNECTING, ACTIVE, RECONNECTING, ENDED }

data class LiveWorkoutUiState(
    val callId: String = "",
    val userId: String = "",
    val isHost: Boolean = false,
    val callStatus: CallStatus = CallStatus.INITIALIZING,
    val localRepCount: Int = 0,
    val remoteRepCounts: Map<String, Int> = emptyMap(),
    val exerciseType: String = "Squats",
    val poseDetected: Boolean = false,
    val errorMessage: String? = null,
    val isMuted: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val callDurationSeconds: Int = 0,
    val isInitError: Boolean = false
)

class LiveWorkoutViewModel(
    application: Application,
    private val serverId: String
) : AndroidViewModel(application) {

    private val signalingService = WebRtcSignalingService()
    private val repCounter = RepCounter()
    private val poseAnalyzer = PoseAnalyzer(application)

    private val _uiState = MutableStateFlow(LiveWorkoutUiState())
    val uiState: StateFlow<LiveWorkoutUiState> = _uiState.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _webRtcManagerReady = MutableStateFlow(false)
    val webRtcManagerReady: StateFlow<Boolean> = _webRtcManagerReady.asStateFlow()

    var webRtcManager: WebRtcManager? = null
        private set

    private val currentCallId get() = _uiState.value.callId
    private val isHost get() = _uiState.value.isHost

    private var timerJob: Job? = null
    private var reconnectTimeoutJob: Job? = null

    init {
        poseAnalyzer.initialize { poseResult ->
            val exerciseType = _uiState.value.exerciseType
            val repCounted = repCounter.update(poseResult, exerciseType)
            _uiState.update { it.copy(poseDetected = poseResult.isBodyFullyVisible) }
            if (repCounted) {
                val newCount = repCounter.getCount()
                _uiState.update { it.copy(localRepCount = newCount) }
                val callId = currentCallId
                val userId = _uiState.value.userId
                if (callId.isNotEmpty() && userId.isNotEmpty()) {
                    viewModelScope.launch {
                        runCatching {
                            signalingService.updateRepCount(callId, userId, newCount, exerciseType)
                        }
                    }
                }
            }
        }
    }

    fun setup(userId: String) {
        _uiState.update { it.copy(userId = userId) }
        try {
            val manager = WebRtcManager(
                context = getApplication(),
                poseAnalyzer = poseAnalyzer,
                onLocalIceCandidate = { candidate -> handleLocalIceCandidate(candidate) },
                onRemoteVideoTrack = { track -> _remoteVideoTrack.value = track },
                onConnectionStateChange = { state -> handleIceStateChange(state) }
            )
            manager.initPeerConnection()
            webRtcManager = manager
            _uiState.update { it.copy(isFrontCamera = manager.isFrontCamera) }
            _webRtcManagerReady.value = true
        } catch (e: Throwable) {
            _uiState.update {
                it.copy(
                    isInitError = true,
                    errorMessage = "Video call unavailable: ${e.message}"
                )
            }
            return
        }

        viewModelScope.launch {
            runCatching {
                val activeCallId = signalingService.getActiveCallForServer(serverId)
                if (activeCallId != null) {
                    joinAsGuest(activeCallId)
                } else {
                    startAsHost(userId)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun handleIceStateChange(state: PeerConnection.IceConnectionState) {
        when (state) {
            PeerConnection.IceConnectionState.CONNECTED,
            PeerConnection.IceConnectionState.COMPLETED -> {
                reconnectTimeoutJob?.cancel()
                _uiState.update { it.copy(callStatus = CallStatus.ACTIVE) }
                startCallTimer()
            }
            // DISCONNECTED is transient — the connection may recover. Start a timeout
            // instead of immediately ending the call.
            PeerConnection.IceConnectionState.DISCONNECTED -> {
                _uiState.update { it.copy(callStatus = CallStatus.RECONNECTING) }
                scheduleReconnectTimeout()
            }
            PeerConnection.IceConnectionState.FAILED,
            PeerConnection.IceConnectionState.CLOSED -> {
                timerJob?.cancel()
                reconnectTimeoutJob?.cancel()
                _uiState.update { it.copy(callStatus = CallStatus.ENDED) }
            }
            else -> {}
        }
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _uiState.update { it.copy(callDurationSeconds = it.callDurationSeconds + 1) }
            }
        }
    }

    // End the call if still in RECONNECTING after 15 seconds
    private fun scheduleReconnectTimeout() {
        reconnectTimeoutJob?.cancel()
        reconnectTimeoutJob = viewModelScope.launch {
            delay(15_000)
            if (_uiState.value.callStatus == CallStatus.RECONNECTING) {
                timerJob?.cancel()
                _uiState.update { it.copy(callStatus = CallStatus.ENDED) }
            }
        }
    }

    private suspend fun startAsHost(userId: String) {
        val manager = webRtcManager ?: return
        _uiState.update { it.copy(isHost = true, callStatus = CallStatus.WAITING_FOR_PEER) }
        val offerSdp = manager.createOffer()
        val callId = signalingService.createCall(serverId, userId, offerSdp)
        _uiState.update { it.copy(callId = callId) }

        listenForRemoteRepCounts(callId)

        signalingService.listenForAnswer(callId)
            .filterNotNull()
            .first()
            .let { answerSdp ->
                // Set remote description BEFORE starting to listen for guest ICE candidates.
                // Adding ICE candidates before setRemoteDescription silently fails in WebRTC.
                manager.setRemoteAnswer(answerSdp)
                _uiState.update { it.copy(callStatus = CallStatus.CONNECTING) }
                listenForGuestIce(callId)
            }
    }

    private suspend fun joinAsGuest(callId: String) {
        val manager = webRtcManager ?: return
        _uiState.update { it.copy(callId = callId, isHost = false, callStatus = CallStatus.CONNECTING) }

        val signal = signalingService.getCallSignal(callId)
        if (signal == null || signal.status == "ended") {
            // Set ENDED so the LaunchedEffect in LiveWorkoutScreen navigates back automatically
            _uiState.update { it.copy(
                callStatus = CallStatus.ENDED,
                errorMessage = "Live session not found or has ended"
            ) }
            return
        }

        manager.setRemoteOffer(signal.offer)
        val answerSdp = manager.createAnswer()
        signalingService.saveAnswer(callId, answerSdp)

        listenForHostIce(callId)
        listenForRemoteRepCounts(callId)
    }

    private fun listenForHostIce(callId: String) {
        viewModelScope.launch {
            signalingService.listenForHostIceCandidates(callId).collect { c ->
                webRtcManager?.addRemoteIceCandidate(c.sdpMid, c.sdpMLineIndex, c.candidate)
            }
        }
    }

    private fun listenForGuestIce(callId: String) {
        viewModelScope.launch {
            signalingService.listenForGuestIceCandidates(callId).collect { c ->
                webRtcManager?.addRemoteIceCandidate(c.sdpMid, c.sdpMLineIndex, c.candidate)
            }
        }
    }

    private fun listenForRemoteRepCounts(callId: String) {
        val myUserId = _uiState.value.userId
        viewModelScope.launch {
            signalingService.listenForRepCounts(callId).collect { counts ->
                val remoteMap = counts
                    .filter { it.userId != myUserId }
                    .associate { it.userId to it.repCount }
                _uiState.update { it.copy(remoteRepCounts = remoteMap) }
            }
        }
    }

    private fun handleLocalIceCandidate(candidate: IceCandidate) {
        val callId = currentCallId
        if (callId.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val data = IceCandidateData(
                    sdpMid = candidate.sdpMid ?: "",
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    candidate = candidate.sdp ?: ""
                )
                if (isHost) {
                    signalingService.addHostIceCandidate(callId, data)
                } else {
                    signalingService.addGuestIceCandidate(callId, data)
                }
            }
        }
    }

    fun setExerciseType(exerciseType: String) {
        _uiState.update { it.copy(exerciseType = exerciseType) }
        repCounter.reset()
        _uiState.update { it.copy(localRepCount = 0) }
    }

    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        webRtcManager?.muteAudio(newMuted)
        _uiState.update { it.copy(isMuted = newMuted) }
    }

    fun toggleCamera() {
        val newEnabled = !_uiState.value.isCameraEnabled
        webRtcManager?.enableVideo(newEnabled)
        _uiState.update { it.copy(isCameraEnabled = newEnabled) }
    }

    fun switchCamera() {
        webRtcManager?.switchCamera { isFront ->
            _uiState.update { it.copy(isFrontCamera = isFront) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun endCall() {
        timerJob?.cancel()
        reconnectTimeoutJob?.cancel()
        viewModelScope.launch {
            val callId = currentCallId
            if (callId.isNotEmpty()) {
                runCatching { signalingService.endCall(callId, serverId) }
            }
            _uiState.update { it.copy(callStatus = CallStatus.ENDED) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        reconnectTimeoutJob?.cancel()
        webRtcManager?.release()
        poseAnalyzer.close()
    }

    companion object {
        fun factory(application: Application, serverId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    LiveWorkoutViewModel(application, serverId) as T
            }
    }
}
