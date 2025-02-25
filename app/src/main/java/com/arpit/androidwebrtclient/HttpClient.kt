package com.arpit.androidwebrtclient

import android.util.Log
import okhttp3.Call

import okhttp3.Callback

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType

import okhttp3.OkHttpClient

import okhttp3.Request

import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

import okhttp3.Response
import java.io.IOException

class HttpClient {


    private val tag = "Test:HttpClient"

    private val client: OkHttpClient = OkHttpClient()
    private val url = "https://api-dt1-dev-aps1.lightmetrics.co:3478/"

    private var iceServerString = ""

    private val gson: com.google.gson.Gson = com.google.gson.Gson()

    fun getIceServers() {
        Log.d(tag, "Get Ice Servers")
        val request: Request = Request.Builder()
            .url(url + "IceServers")
            .build()

//        final IceServerModel[] iceServers = new IceServerModel[1];
//        final String[] responseData = new String[1];
        client.newCall(request).enqueue(object : Callback {
            // Async call
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.d(tag, "Server Error: " + response.code)
                    return
                }
                val responseStr: String? = response.body?.string()
                Log.d(tag, "Response: $response")
                if (responseStr != null) {
                    iceServerString = responseStr
                }
                //                iceServers[0] = gson.fromJson(responseData[0], IceServerModel.class);
//                Log.d(tag, "Response After Parsing " + iceServers[0]);
            }
        })
        Log.d(tag, "Response: $iceServerString")
    }

    fun registerSession(sessionId: String, peerId: String) {
        Log.d(tag, "Register Session")
        val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
        //        String jsonBody = "{ \"name\": \"John Doe\", \"age\": 30 }";
        val jsonBody = ""
        val body: RequestBody = jsonBody.toRequestBody(JSON)

        val request: Request = Request.Builder()
            .url(url + "register/" + sessionId + "/" + peerId)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.d(tag, "Server Error: " + response.code)
                    return
                }
                Log.d(tag, "Session Registered Successfully")
            }
        })
    }

    fun sendMessage(
        sessionId: String,
        peerId: String,
        message: String
    ) {
        val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody: RequestBody = message.toRequestBody(JSON)
        val request: Request = Request.Builder()
            .url(url + "message/" + sessionId + "/" + peerId)
            .post(requestBody)
            .build()
        Log.d(tag, "sendOffer: request: $request")
        client.newCall(request).enqueue (object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.d(tag, "Server Error: " + response.code)
                    return
                }
                Log.d(tag, "Send Offer Successfully")
            }
        })
    }

    fun getIceServerString(): String? {
        return iceServerString
    }

}