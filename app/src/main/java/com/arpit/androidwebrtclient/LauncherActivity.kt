package com.arpit.androidwebrtclient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LauncherActivity : AppCompatActivity() {

    private val tag = "Test:LauncherActivity"
//    private val CAMERA_PERMISSION_CODE = 100
    private val PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)


        // Check and request camera permission
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
//        }

        // Check and request audio permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET), PERMISSION_CODE)
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val httpClient = HttpClient()
        httpClient.getIceServers()
//        Log.d(tag, "ice server data response:" + responseData);


        //        Log.d(tag, "ice server data response:" + responseData);
        val startSession = findViewById<Button>(R.id.start_session)

        // when someone clicks on startSession button it should call CompleteActivity class and pass
        // iceServers and start session to that class

        // when someone clicks on startSession button it should call CompleteActivity class and pass
        // iceServers and start session to that class
        startSession.setOnClickListener { v: View? ->
            Log.d(tag, "ice server data response:" + httpClient.getIceServerString()
            )
            val intent = Intent(
                this@LauncherActivity,
                CompleteActivity::class.java
            )
            intent.putExtra("iceServers", httpClient.getIceServerString())
            intent.putExtra("request", "startSession")
            startActivity(intent)
        }

        // create join_session button here, whenever someone click join_session it should read text
        // from get_session_id and create CompleteActivity with iceServers, request = "joinSession" and sessionId

        // read the sessionId from get_session_id edit text box


        // create join_session button here, whenever someone click join_session it should read text
        // from get_session_id and create CompleteActivity with iceServers, request = "joinSession" and sessionId

        // read the sessionId from get_session_id edit text box
        val joinSession = findViewById<Button>(R.id.join_session)
        joinSession.setOnClickListener { v: View? ->
            val sessionIdEditText = findViewById<EditText>(R.id.get_session_id)
            // read sessionId from sessionIdEditText
            val sessionId = sessionIdEditText.text.toString()
            Log.d(tag,
                "sessioEditaText: " + sessionIdEditText + "sessionId: " + sessionId + " length: " + sessionIdEditText.length())
            Log.d(tag, "ice server data response:" + httpClient.getIceServerString())
            val intent = Intent(
                this@LauncherActivity,
                CompleteActivity::class.java
            )
            intent.putExtra("iceServers", httpClient.getIceServerString())
            intent.putExtra("request", "joinSession")
            intent.putExtra("sessionId", sessionId)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
//            CAMERA_PERMISSION_CODE -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Log.d(tag, "Got Camera Permission")
//                } else {
//                    Log.d(tag, "Camera Permission Denied")
//
//                    // Permission denied, handle accordingly
//                }
//            }

            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(tag, "Got Permissions $permissions")

                    // Permission granted, proceed with audio usage
                } else {
                    Log.d(tag, "Audio Permission Denied")

                    // Permission denied, handle accordingly
                }
            }
        }
    }
}