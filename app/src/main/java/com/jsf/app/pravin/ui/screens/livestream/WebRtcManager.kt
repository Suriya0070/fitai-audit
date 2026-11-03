package com.jsf.app.pravin.ui.screens.livestream

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import com.jsf.app.pravin.domain.PoseAnalyzer
import org.webrtc.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class WebRtcManager(
    private val context: Context,
    private val poseAnalyzer: PoseAnalyzer,
    private val onLocalIceCandidate: (IceCandidate) -> Unit,
    private val onRemoteVideoTrack: (VideoTrack) -> Unit,
    private val onConnectionStateChange: (PeerConnection.IceConnectionState) -> Unit
) {
    companion object {
        private val factoryInitialized = AtomicBoolean(false)
    }

    val eglBase: EglBase = EglBase.create()

    private var frameCount = 0
    private val poseSink = VideoSink { frame ->
        if (frameCount++ % 6 == 0) {
            frame.retain()
            try {
                convertToBitmap(frame.buffer, 4)?.let { bmp ->
                    poseAnalyzer.analyze(bmp, frame.timestampNs / 1_000_000)
                }
            } finally {
                frame.release()
            }
        }
    }

    private val peerObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let { onLocalIceCandidate(it) }
        }
        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
            val track = receiver?.track()
            if (track is VideoTrack) onRemoteVideoTrack(track)
        }
        override fun onAddStream(stream: MediaStream?) {
            stream?.videoTracks?.firstOrNull()?.let { onRemoteVideoTrack(it) }
        }
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            state?.let { onConnectionStateChange(it) }
        }
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onRemoveStream(p0: MediaStream?) {}
        override fun onDataChannel(p0: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
    }

    private val factory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null

    private val videoSource: VideoSource
    val localVideoTrack: VideoTrack
    private val audioSource: AudioSource
    val localAudioTrack: AudioTrack
    private val capturer: CameraVideoCapturer
    private val helper: SurfaceTextureHelper

    var isFrontCamera: Boolean = true
        private set

    init {
        if (factoryInitialized.compareAndSet(false, true)) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions
                    .builder(context.applicationContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )
        }

        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()

        videoSource = factory.createVideoSource(false)
        localVideoTrack = factory.createVideoTrack("v0", videoSource)
        localVideoTrack.setEnabled(true)

        audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("a0", audioSource)
        localAudioTrack.setEnabled(true)

        helper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)

        val enumerator = Camera2Enumerator(context)
        val cameraId = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: throw IllegalStateException("No camera available on this device")
        isFrontCamera = enumerator.isFrontFacing(cameraId)

        capturer = Camera2Capturer(context, cameraId, null)
        capturer.initialize(helper, context.applicationContext, videoSource.capturerObserver)
        try {
            capturer.startCapture(640, 480, 30)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        localVideoTrack.addSink(poseSink)
    }

    fun initPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            // Public TURN for NAT traversal — replace with a dedicated TURN server in production
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        peerConnection = factory.createPeerConnection(config, peerObserver)
        // addTrack (UNIFIED_PLAN) — addStream is deprecated with UNIFIED_PLAN semantics
        peerConnection?.addTrack(localVideoTrack, listOf("local_stream"))
        peerConnection?.addTrack(localAudioTrack, listOf("local_stream"))
    }

    fun muteAudio(muted: Boolean) {
        localAudioTrack.setEnabled(!muted)
    }

    fun enableVideo(enabled: Boolean) {
        localVideoTrack.setEnabled(enabled)
    }

    fun switchCamera(onSwitched: (isFront: Boolean) -> Unit = {}) {
        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                isFrontCamera = isFront
                onSwitched(isFront)
            }
            override fun onCameraSwitchError(errorDescription: String?) {}
        })
    }

    suspend fun createOffer(): String = suspendCancellableCoroutine { cont ->
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) { if (cont.isActive) cont.resumeWithException(Exception("null offer")); return }
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() { if (cont.isActive) cont.resume(sdp.description) }
                    override fun onSetFailure(e: String?) { if (cont.isActive) cont.resumeWithException(Exception(e)) }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
            }
            override fun onCreateFailure(e: String?) { if (cont.isActive) cont.resumeWithException(Exception(e)) }
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }

    suspend fun setRemoteOffer(offerSdp: String) = suspendCancellableCoroutine<Unit> { cont ->
        val sdp = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() { if (cont.isActive) cont.resume(Unit) }
            override fun onSetFailure(e: String?) { if (cont.isActive) cont.resumeWithException(Exception(e)) }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }

    suspend fun createAnswer(): String = suspendCancellableCoroutine { cont ->
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) { if (cont.isActive) cont.resumeWithException(Exception("null answer")); return }
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() { if (cont.isActive) cont.resume(sdp.description) }
                    override fun onSetFailure(e: String?) { if (cont.isActive) cont.resumeWithException(Exception(e)) }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
            }
            override fun onCreateFailure(e: String?) { if (cont.isActive) cont.resumeWithException(Exception(e)) }
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }

    suspend fun setRemoteAnswer(answerSdp: String) = suspendCancellableCoroutine<Unit> { cont ->
        val sdp = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() { if (cont.isActive) cont.resume(Unit) }
            override fun onSetFailure(e: String?) { if (cont.isActive) cont.resumeWithException(Exception(e)) }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        peerConnection?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
    }

    fun release() {
        runCatching { capturer.stopCapture() }
        runCatching { capturer.dispose() }
        runCatching { helper.dispose() }
        runCatching { localVideoTrack.removeSink(poseSink) }
        runCatching { localVideoTrack.dispose() }
        runCatching { localAudioTrack.dispose() }
        runCatching { videoSource.dispose() }
        runCatching { audioSource.dispose() }
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        runCatching { factory.dispose() }
        runCatching { eglBase.release() }
    }

    private fun convertToBitmap(buffer: VideoFrame.Buffer, scale: Int): android.graphics.Bitmap? {
        val i420 = buffer.toI420() ?: return null
        return try {
            val w = (i420.width / scale).coerceAtLeast(1)
            val h = (i420.height / scale).coerceAtLeast(1)
            val nv21 = ByteArray(w * h * 3 / 2)
            for (row in 0 until h) {
                for (col in 0 until w) {
                    nv21[row * w + col] = i420.dataY.get(row * scale * i420.strideY + col * scale)
                }
            }
            val uvBase = w * h
            for (row in 0 until h / 2) {
                for (col in 0 until w / 2) {
                    val idx = uvBase + row * w + col * 2
                    nv21[idx] = i420.dataV.get(row * scale * i420.strideV + col * scale)
                    nv21[idx + 1] = i420.dataU.get(row * scale * i420.strideU + col * scale)
                }
            }
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, w, h, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, w, h), 75, out)
            val bytes = out.toByteArray()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        } finally {
            i420.release()
        }
    }
}
