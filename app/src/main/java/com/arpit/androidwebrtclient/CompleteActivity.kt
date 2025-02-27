package com.arpit.androidwebrtclient

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arpit.androidwebrtclient.databinding.ActivitySamplePeerConnectionBinding
import com.google.gson.Gson
import com.launchdarkly.eventsource.EventHandler
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.MessageEvent
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.AddIceObserver
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.EglBase
import org.webrtc.HardwareVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.IceGatheringState
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnection.SignalingState
import org.webrtc.PeerConnectionFactory
import org.webrtc.PeerConnectionFactory.InitializationOptions
import org.webrtc.RtpCapabilities
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SimulcastVideoEncoderFactory
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import java.net.URI

class CompleteActivity : AppCompatActivity() {

    private var tag = "Test:CompleteActivity"
    private val httpClient = HttpClient()


    // video audio config
    private var binding : ActivitySamplePeerConnectionBinding? = null
    private var width = 720
    private var height = 480
    private var fps = 15
    private var localVideoTrack: VideoTrack? = null
    private val videoTrackId = "ARDAMSv0"
    private var localAudioTrack: AudioTrack? = null
    private val audioTrackId = "Track_Audio"

    // webrtc config
    private var rootEglBase: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    // signalling server config
    private var requestType: String = ""
    private var sessionId: String = ""
    private var iceServerModel: IceServerModel? = null
    private var localPeerId: String = ""
    private var remotePeerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_peer_connection)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_peer_connection)
        // from the bundle read iceServers, request and sessionId
        val extras = intent.extras
        if (extras != null) {
            // Extract the values from the Bundle
            val iceServers = extras.getString("iceServers")
            requestType = extras.getString("request") ?: ""
            if (requestType == "joinSession") sessionId = extras.getString("sessionId") ?: ""
            iceServerModel = Gson().fromJson(iceServers, IceServerModel::class.java)
            Log.d(tag, "IceServers Converted: $iceServerModel")
            Log.d(tag, "Request: $requestType")
            Log.d(tag, "SessionId: $sessionId")
        }

        setSupportActionBar(binding?.toolbar)
        start()
    }

    private fun start() {

        // to show local and remote views
        initializeSurfaceViews()

        // creating peerConnectionFactory
        initializePeerConnectionFactory()

        // initialize media streams
        initializeMediaStreams()

        initializePeerConnections()

        startStreamingMedia()


        if (requestType.equals("startSession", ignoreCase = true)) {
            registerSession()
            openSSEConnection()
        } else if (requestType.equals("joinSession", ignoreCase = true)) {
            registerSession()
            openSSEConnection()
        }
    }

    // media streams initialization
    private fun initializeSurfaceViews() {
        rootEglBase = EglBase.create()

        // local
        binding?.surfaceView?.init(rootEglBase?.eglBaseContext, null)
        binding?.surfaceView?.setEnableHardwareScaler(true)
        binding?.surfaceView?.setMirror(true)

        // remote
        binding?.surfaceView2?.init(rootEglBase?.eglBaseContext, null)
        binding?.surfaceView2?.setEnableHardwareScaler(true)
        binding?.surfaceView2?.setMirror(true)
        //add one more
    }
    private fun initializeMediaStreams() {
        val audioConstraints = MediaConstraints()
        val videoCapturer: VideoCapturer? = createVideoCapturer()
        val videoSource = factory?.createVideoSource(false) // false for camera capture
        val surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", rootEglBase!!.eglBaseContext)
        videoCapturer?.initialize(
            surfaceTextureHelper,
            applicationContext,
            videoSource?.capturerObserver
        )
        videoCapturer?.startCapture(width, height, fps)
        localVideoTrack =
            factory?.createVideoTrack(videoTrackId, videoSource)
        localVideoTrack?.setEnabled(true)
        localVideoTrack?.addSink(binding?.surfaceView)

        //create an AudioSource instance
        val audioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack(audioTrackId, audioSource)
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val videoCapturer: VideoCapturer?
        if (useCamera2()) {
            videoCapturer = createCameraCapturer(Camera2Enumerator(this))
        } else {
            videoCapturer = createCameraCapturer(Camera1Enumerator(true))
        }
        return videoCapturer
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    private fun useCamera2(): Boolean {
        return Camera2Enumerator.isSupported(this)
    }

    private fun startStreamingMedia() {
        peerConnection!!.addTrack(localVideoTrack)
        peerConnection!!.addTrack(localAudioTrack)

        Log.d(tag, "startStreamingVideo: added Tracks")
    }

    private fun getH264Codec(factory: PeerConnectionFactory?): List<RtpCapabilities.CodecCapability>? {
        val capabilities = factory?.getRtpSenderCapabilities(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO)
        return capabilities?.codecs?.filter { it.name.equals("H264", ignoreCase = true) }
    }


    // webrtc initialization
    private fun initializePeerConnectionFactory() {
        val options = InitializationOptions.builder(this)
            .setEnableInternalTracer(true)
            //                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val hardwareEncoder = HardwareVideoEncoderFactory(rootEglBase?.eglBaseContext, true, true)
        SimulcastVideoEncoderFactory(hardwareEncoder, SoftwareVideoEncoderFactory())

        factory =
            PeerConnectionFactory.builder()
                .setVideoEncoderFactory(SimulcastVideoEncoderFactory(hardwareEncoder, SoftwareVideoEncoderFactory()))
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext))
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()
        Log.d(tag, "Peer Connection Factory Created: $factory")
    }

    private fun initializePeerConnections() {
        peerConnection = createPeerConnection(factory)
        Log.d(
            tag,
            "Peer Connection Created, iceConnectionState: " + peerConnection?.iceConnectionState()
        )
        Log.d(
            tag,
            "Peer Connection Created, iceGatheringState: " + peerConnection?.iceGatheringState()
        )
    }

    private fun createPeerConnection(factory: PeerConnectionFactory?): PeerConnection? {
        val iceServers = ArrayList<IceServer>()
        if (iceServerModel == null) {
            Log.d(tag, "Ice Servers Missing")
        }
        val urls = iceServerModel!!.getUrls()
        for (url in urls!!) {
            iceServers.add(
                IceServer.builder(url)
                    .setUsername(iceServerModel!!.getUsername())
                    .setPassword(iceServerModel!!.getCredential())
                    .createIceServer()
            )
        }
        val rtcConfig = RTCConfiguration(iceServers)
        Log.d(tag, "Peer Connection Created: rtcConfig: $rtcConfig")

        val pcObserver: PeerConnection.Observer = object : PeerConnection.Observer {
            override fun onSignalingChange(signalingState: SignalingState) {
                Log.d(
                    tag,
                    "onSignalingChange: signalingState: $signalingState"
                )
            }

            override fun onStandardizedIceConnectionChange(newState: IceConnectionState?) {
                super.onStandardizedIceConnectionChange(newState)
                Log.d(
                    tag,
                    "onStandardizedIceConnectionChange $newState"
                )
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)

                Log.d(tag, "onConnectionChange: $newState")
            }

            override fun onIceCandidateError(event: IceCandidateErrorEvent?) {
                super.onIceCandidateError(event)

                Log.d(tag, "onIceCandidateError: $event")
            }

            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
                super.onSelectedCandidatePairChanged(event)

                Log.d(tag, "onSelectedCandidatePairChanged: $event")
            }

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                super.onAddTrack(receiver, mediaStreams)
                Log.d(tag, "onAddTrack: $mediaStreams")
            }

            override fun onRemoveTrack(receiver: RtpReceiver?) {
                super.onRemoveTrack(receiver)

                Log.d(tag, "onRemoveTrack: $receiver")
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                super.onTrack(transceiver)
                Log.d(tag, "onTrack: $transceiver")
            }

            override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
                Log.d(tag, "onIceConnectionChange: ")
            }

            override fun onIceConnectionReceivingChange(b: Boolean) {
                Log.d(tag, "onIceConnectionReceivingChange: ")
            }

            override fun onIceGatheringChange(iceGatheringState: IceGatheringState) {
                Log.d(tag, "onIceGatheringChange: $iceGatheringState")
            }

            override fun onIceCandidate(iceCandidate: IceCandidate) {
                Log.d(tag, "onIceCandidate: ")
                val message = JSONObject()
                try {
                    message.put("type", "ice-candidate")
                    message.put("target", remotePeerId)
                    val payload = JSONObject()
                    payload.put("candidate", iceCandidate.sdp)
                    payload.put("sdpMid", iceCandidate.sdpMid)
                    payload.put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
                    message.put("payload", payload)
                    Log.d(
                        tag,
                        "onIceCandidate: sending candidate $message"
                    )
                    httpClient.sendMessage(sessionId, localPeerId, message.toString())
                    Log.d(
                        tag,
                        "Important: sendMessage, onIceCandidate: message: $message"
                    )
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
                Log.d(tag, "onIceCandidatesRemoved: $iceCandidates")
            }

            override fun onAddStream(mediaStream: MediaStream) {
                Log.d(tag, "onAddStream: videoSize: " + mediaStream.videoTracks?.size +
                            " id: " + mediaStream.id + "audioSize: " + mediaStream.audioTracks?.size)

                if(mediaStream.audioTracks != null && mediaStream.audioTracks.size > 0) {
                    val remoteAudioTrack = mediaStream.audioTracks[0]
                    remoteAudioTrack.setEnabled(true)
                }
                else {
                    Log.d(tag, "Empty Audio Stream Received")
                }
                if(mediaStream.videoTracks != null && mediaStream.videoTracks.size > 0) {
                    val remoteVideoTrack = mediaStream.videoTracks[0]
                    remoteVideoTrack.setEnabled(true)
                    remoteVideoTrack.addSink(binding?.surfaceView2)
                }
                else {
                    Log.d(tag, "Empty Video Stream Received")
                }
            }

            override fun onRemoveStream(mediaStream: MediaStream) {
                Log.d(tag, "onRemoveStream: $mediaStream")
            }

            override fun onDataChannel(dataChannel: DataChannel) {
                Log.d(tag, "onDataChannel: ")
            }

            override fun onRenegotiationNeeded() {
                Log.d(tag, "onRenegotiationNeeded: ")
            }
        }
        Log.d(tag, "Creating Peer Connection")
        return factory?.createPeerConnection(rtcConfig, pcObserver)
    }

    // create webrtc offer
    private fun doCall(senderId: String) {
        val mediaConstraints = MediaConstraints();

        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))


        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                Log.d(tag, "onCreateSuccess: ")
                peerConnection?.setLocalDescription(
                    object : SimpleSdpObserver() {
                        override fun onCreateSuccess(p0: SessionDescription?) {
                            super.onCreateSuccess(p0)
                            Log.d(tag, "onCreateSuccess: ${p0?.description}")
                        }

                        override fun onSetSuccess() {
                            super.onSetSuccess()
                            val message = JSONObject()
                            try {
                                // peerId
                                message.put("target", senderId)
                                message.put("type", "offer")
                                val payload = JSONObject()
                                payload.put("type", "offer")
                                payload.put("sdp", sessionDescription?.description)
                                message.put("payload", payload)
                                // ArpitChange:  need to print here temporarily
                                Log.d(
                                    tag, "sendMessage, sending offer, message: $message"
                                )
                                httpClient.sendMessage(sessionId, localPeerId, message.toString())
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                        override fun onCreateFailure(p0: String?) {
                            super.onCreateFailure(p0)
                            Log.d(tag, "onCreateFailure: ${p0}")

                        }

                        override fun onSetFailure(p0: String?) {
                            super.onSetFailure(p0)
                            Log.d(tag, "onSetFailure: ${p0}")
                        }
                    }, sessionDescription)
                }
        }, mediaConstraints)
    }

    // create webrtc answer
    private fun doAnswer() {
        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
//                val sdpConstraints = MediaConstraints()
                //                sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
//                sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                val message = JSONObject()
                try {
                    message.put("target", remotePeerId)
                    message.put("type", "answer")
                    val payload = JSONObject()
                    payload.put("type", "answer")
                    payload.put("sdp", sessionDescription?.description)
                    message.put("payload", payload)
                    httpClient.sendMessage(sessionId, localPeerId, message.toString())

                    Log.d(tag, "answer sent: $message")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, MediaConstraints())
        Log.d(tag, "Answer Sent, localDescription: " + peerConnection?.localDescription +
                    " remoteDescription: " + peerConnection?.remoteDescription
        )
    }


    // sse initializations
    private fun registerSession() {
        if (sessionId.isEmpty()) {
            sessionId = generateSessionId()
        }
        if(localPeerId.isEmpty()) localPeerId = generatePeerId()
        Log.d(tag, "Registering Session, sessionId: $sessionId peerId: $localPeerId")
        httpClient.registerSession(sessionId, localPeerId)
    }

    private fun openSSEConnection() {
        Log.d(tag,
            "Opening SSE Connection, sessionId: $sessionId localPeerId: $localPeerId"
        )
        startListening(sessionId, localPeerId)
    }

    private fun startListening(sessionId: String, peerId: String) {
        val url =
            "https://api-dt1-dev-aps1.lightmetrics.co:3478/events/$sessionId/$peerId"
        val eventSource = EventSource.Builder(object : EventHandler {
            override fun onOpen() {
                Log.d(tag, "Connected to SSE Server, connection opened")
            }

            @Throws(Exception::class)
            override fun onClosed() {
                Log.d(tag, "Connected to SSE Server, connection closed")
            }

            @Throws(JSONException::class)
            override fun onMessage(event: String, messageEvent: MessageEvent) {
                Log.d(tag, "Event: $event, message: ${messageEvent.data}")

                // sending offer
                val json = JSONObject(messageEvent.data)
                Log.d(tag, "json : $json")
                try {
                    if (json.getString("type") == "new-peer") {
                        remotePeerId = json.getString("senderId")
                        doCall(remotePeerId)
                    }

                    else if (json.getString("type") == "offer") {
                        remotePeerId = json.getString("senderId")
                        val payload = json.getString("payload")
                        Log.d(
                            tag,
                            "got offer: remotePeerId: " + remotePeerId + "offer payload : " + payload
                        )
                        val payloadJson = JSONObject(payload)
                        val sdp = payloadJson.getString("sdp")
                        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                            override fun onSetSuccess() {
                                super.onSetSuccess()
                                Log.d(
                                    tag, "onSetSuccess: " +
                                            " sdp set successfully," +
                                            " remote connection created"
                                )
                            }

                            override fun onSetFailure(s: String?) {
                                super.onSetFailure(s)
                                Log.d(
                                    tag,
                                    "onSetFailure: setting sdp failed: $s"
                                )
                            }
                        }, SessionDescription(SessionDescription.Type.OFFER, sdp))

                        Log.d(
                            tag, "received offer, offer set," +
                                    " iceGatheringState: " + peerConnection?.iceGatheringState()
                                    + " remoteSDP: " + peerConnection?.remoteDescription +
                                    " localSdp: " + peerConnection?.localDescription
                        )
                        doAnswer()
                    }

                    else if (json.getString("type") == "answer") {
                        val payload = json.getString("payload")
                        Log.d(tag, "answer payload : $payload")
                        val payloadJson = JSONObject(payload)
                        val sdp = payloadJson.getString("sdp")
                        Log.d(tag, "answer sdp : $sdp")
                        peerConnection!!.setRemoteDescription(
                            object : SimpleSdpObserver() {
                                override fun onSetSuccess() {
                                    super.onSetSuccess()
                                    Log.d(
                                        tag,
                                        "onSetSuccess: sdp set successfully"
                                    )
                                }

                                override fun onSetFailure(s: String?) {
                                    super.onSetFailure(s)
                                    Log.d(
                                        tag,
                                        "onSetFailure: sdp failed$s"
                                    )
                                }
                            },
                            SessionDescription(SessionDescription.Type.ANSWER, sdp)
                        )
                        Log.d(
                            tag, "received answer," +
                                    " iceGatheringState: " + peerConnection!!.iceGatheringState()
                                    + " remoteSDP: " + peerConnection!!.remoteDescription +
                                    " localSdp: " + peerConnection!!.localDescription
                        )
                    }

                    else if (json.getString("type") == "ice-candidate") {
                        val payload = json.getString("payload")
                        //                            Log.d(TAG, "iceCandidate payload : " + payload);
                        val payloadJson = JSONObject(payload)
                        val candidate = payloadJson.getString("candidate")
                        val sdpMid = payloadJson.getString("sdpMid")
                        val sdpMLineIndex = payloadJson.getString("sdpMLineIndex").toInt()
                        //                            Log.d(TAG, "Important sdpMid: " + sdpMid +
//                                    " sdpMLineIndex: " + sdpMLineIndex + " candidate: " + candidate);
                        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
                        peerConnection!!.addIceCandidate(iceCandidate, object : AddIceObserver {
                            override fun onAddSuccess() {
                                Log.d(tag, "Ice Candidate Added Successfully")
                            }

                            override fun onAddFailure(s: String) {
                                Log.d(
                                    tag,
                                    "Error Adding Ice Candidate: $s"
                                )
                            }
                        })
                    }

                } catch (e: Exception) {
                    Log.d(tag, "Exception In Json Parsing, e: " + e.message)
                }
            }

            @Throws(Exception::class)
            override fun onComment(comment: String) {
                Log.d(tag, "onComment: $comment")
            }

            override fun onError(t: Throwable) {
                t.printStackTrace()
            }
        }, URI.create(url)).build()

        eventSource.start()
    }


    private fun generateSessionId(): String {
        val peerId = StringBuilder()
        peerId.append("session1")
        return peerId.toString()
//            for (int i = 0; i < LENGTH; i++) {
//                peerId.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
//            }
//            return peerId.toString();
    }

    private fun generatePeerId(): String {
        val peerId = java.lang.StringBuilder()
        peerId.append("peer1")
        return peerId.toString()
//            for (int i = 0; i < LENGTH; i++) {
//                peerId.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
//            }
//            return peerId.toString();
    }



}