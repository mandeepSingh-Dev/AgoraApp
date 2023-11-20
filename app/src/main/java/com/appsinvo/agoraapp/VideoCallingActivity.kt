package com.appsinvo.agoraapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.appsinvo.agoraapp.databinding.ActivityVideoCallingBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas


class VideoCallingActivity : AppCompatActivity() {

    var _binding : ActivityVideoCallingBinding? = null
    val binding get()  = _binding!!

    val permissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA
    )


    // An integer that identifies the local user.
    private val uid = 0
    private var isJoined = false

    private var agoraEngine: RtcEngine? = null

    //SurfaceView to render local video in a Container.
    private var localSurfaceView: SurfaceView? = null

    //SurfaceView to render Remote video in a Container.
     var remoteSurfaceView: SurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityVideoCallingBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setupVideoSDKEngine()

        binding.JoinButton.setOnClickListener {
            joinChannel(it)
        }
        binding.LeaveButton.setOnClickListener {
            leaveChannel(it)
        }



    }


    fun checkSelfPermissions() : Boolean
    {
        if(ActivityCompat.checkSelfPermission(this,permissions[0]) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,permissions[1]) != PackageManager.PERMISSION_GRANTED){
            return false
        }

        return true
    }

    val mRtcEventHandler = object : IRtcEngineEventHandler(){
        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)

            runOnUiThread { setupRemoteVideo(uid)

            }



        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)

            runOnUiThread { remoteSurfaceView!!.visibility = View.GONE }

        }
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = Constants.appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine?.enableVideo()
        } catch (e: Exception) {
            Log.d("kndknfd",e.message.toString())
          //  showMessage(e.toString())
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView?.setZOrderMediaOverlay(true)
        container.addView(remoteSurfaceView)
        agoraEngine?.setupRemoteVideo( VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid ) )

        // Display RemoteSurfaceView.
        remoteSurfaceView?.setVisibility(View.VISIBLE)
    }

    private fun setupLocalVideo() {
        val container = findViewById<FrameLayout>(R.id.local_video_view_container)
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(baseContext)
        container.addView(localSurfaceView)
        // Call setupLocalVideo with a VideoCanvas having uid set to 0.
        agoraEngine?.setupLocalVideo( VideoCanvas( localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0 )
        )
    }


    fun joinChannel(view: View?) {
        if (checkSelfPermissions()) {
            val options = ChannelMediaOptions()

            // For a Video call, set the channel profile as COMMUNICATION.
            options.channelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION
            // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
            options.clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
            // Display LocalSurfaceView.
            setupLocalVideo()
            localSurfaceView?.visibility = View.VISIBLE
            // Start local preview.
            agoraEngine?.startPreview()
            // Join the channel with a temp token.
            // You need to specify the user ID yourself, and ensure that it is unique in the channel.
            agoraEngine?.joinChannel(Constants.token, Constants.channelName, uid, options)
        } else {
            Toast.makeText(applicationContext, "Permissions was not granted", Toast.LENGTH_SHORT).show()
        }
    }

    fun leaveChannel(view: View?) {
        if (!isJoined) {
          //  showMessage("Join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
           // showMessage("You left the channel")
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView?.visibility = View.GONE
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView?.visibility = View.GONE
            isJoined = false
        }
    }

}