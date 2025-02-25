package com.arpit.androidwebrtclient

import org.webrtc.SdpObserver

open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(p0: org.webrtc.SessionDescription?) {
        println("onCreateSuccess")
    }

    override fun onSetSuccess() {
        println("onSetSuccess")
    }

    override fun onCreateFailure(p0: String?) {
        println("onCreateFailure")
    }

    override fun onSetFailure(p0: String?) {
        println("onSetFailure")
    }
}