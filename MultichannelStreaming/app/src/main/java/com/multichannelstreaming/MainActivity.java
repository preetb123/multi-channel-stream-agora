package com.multichannelstreaming;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.ChannelMediaOptions;

import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.RtcEngineEx;

import android.widget.Button;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    // Fill the App ID of your project generated on Agora Console.
    private final String appId = "9b82a4a719e24fe280fe652fe06f2f6b";
    // Fill the channel name.
    private String channelName = "channel1";
    // Fill the temp token generated on Agora Console.
    private String token = "007eJxTYFiscpajrudtqkaJi8kKu507jpyeMSnLxGc9R7LbsqBLn90VGCyTLIwSTRLNDS1TjUzSUo0sDNJSzUyN0lINzNKM0sySZJZ4pjQEMjK8/7uQgREIWYAYxGcCk8xgkgVMcjAkZyTm5aXmGDIwAAAktyPc";
    // An integer that identifies the local user.
    private int uid = 12345;
    private boolean isJoined = false;

    private RtcEngineEx agoraEngine;

    //SurfaceView to render local video in a Container.
    private SurfaceView localSurfaceView;
    //SurfaceView to render Remote video in a Container.
    private SurfaceView remoteSurfaceView;
    // A toggle switch to change the User role.
    private Switch audienceRole;

    private Button secondChannelButton;
    private RtcConnection rtcSecondConnection;
    private String secondChannelName = "channel2";
    private int secondChannelUid = uid; // Uid for the second channel
    private String secondChannelToken = "007eJxTYDgWu7a2akXxVI5rmX75F5Q/Bmk/4958hKHcYmG+1GeXpO8KDJZJFkaJJonmhpapRiZpqUYWBmmpZqZGaakGZmlGaWZJ89d4pjQEMjKY8sxjZmRgZGABYhCfCUwyg0kWMMnBkJyRmJeXmmPEwAAAGv4jAA==";
    private boolean isSecondChannelJoined = false; // Track connection state of the second channel



    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audienceRole = (Switch) findViewById(R.id.switch1);
        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
        setupVideoSDKEngine();

        secondChannelButton = findViewById(R.id.secondChannelButton);
        secondChannelButton.setEnabled(false);

    }

    private void setupVideoSDKEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            agoraEngine = (RtcEngineEx) RtcEngine.create(config);

            // By default, the video module is disabled, call enableVideo to enable it.
            //agoraEngine.enableVideo();
        } catch (Exception e) {
            showMessage(e.toString());
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        agoraEngine.stopPreview();
        agoraEngine.leaveChannel();

        // Destroy the engine in a sub-thread to avoid congestion
        new Thread(() -> {
            RtcEngine.destroy();
            agoraEngine = null;
        }).start();
    }

    public void joinSecondChannel(View view) {

        if (isSecondChannelJoined) {
            agoraEngine.leaveChannelEx(rtcSecondConnection);

        } else {
            ChannelMediaOptions mediaOptions = new ChannelMediaOptions();

            if (audienceRole.isChecked()) { // Audience Role
                mediaOptions.autoSubscribeAudio = true;
                mediaOptions.autoSubscribeVideo = true;
                mediaOptions.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE;
            } else { // Host Role
                mediaOptions.publishCameraTrack = true;
                mediaOptions.publishMicrophoneTrack = true;
                mediaOptions.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
                mediaOptions.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            }

            rtcSecondConnection = new RtcConnection();
            rtcSecondConnection.channelId = secondChannelName;
            rtcSecondConnection.localUid = secondChannelUid;

            agoraEngine.joinChannelEx(secondChannelToken, rtcSecondConnection, mediaOptions, secondChannelEventHandler);
            isSecondChannelJoined = true;
        }
    }

    private Set<Integer> channel1Users = new LinkedHashSet<>();
    private Set<Integer> channel2Users = new LinkedHashSet<>();

    private void updateInfo(){
        String channel1Text = "Channel1 Status: " + (isJoined ? "Joined" : "Not joined");
        StringBuilder sb = new StringBuilder(channel1Text);
        sb.append("\nUsers: \n");
        for(Integer s: channel1Users){
            sb.append(s).append("\n");
        }

        String channel2Text = "\nChannel2 Status: " + (isSecondChannelJoined ? "Joined" : "Not joined");
        StringBuilder sb2 = new StringBuilder(channel2Text);
        sb2.append("\nUsers: \n");
        for(Integer s: channel2Users){
            sb2.append(s).append("\n");
        }

        TextView groupInfo = (TextView) findViewById(R.id.group_info);
        groupInfo.setText(sb.toString() + "\n" + sb2.toString());
    }

    // Callbacks for the second channel
    private final IRtcEngineEventHandler secondChannelEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            showMessage(String.format("onJoinChannelSuccess channel %s uid %d", secondChannelName, uid));

            isSecondChannelJoined = true;
            channel2Users.add(uid);
            runOnUiThread(() -> {
                secondChannelButton.setText("Leave Second Channel");
                updateInfo();
            });
        }

        public void onLeaveChannel(RtcStats stats) {
            isSecondChannelJoined = false;
            showMessage("Left the channel " + secondChannelName + ", " + stats.users);
            channel2Users.remove(stats.users);
            runOnUiThread(() -> {
                secondChannelButton.setText("Join Second Channel");
                updateInfo();
            });
            channel2Users.clear();
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            showMessage(String.format("user %d joined!", uid));

            channel2Users.add(uid);

            // Create surfaceView for remote video
            remoteSurfaceView = new SurfaceView(getBaseContext());
            remoteSurfaceView.setZOrderMediaOverlay(true);

            FrameLayout container = findViewById(R.id.remote_video_view_container);

            // Add surfaceView to the container
            runOnUiThread(() -> {
                if (container.getChildCount() > 0) container.removeAllViews();
                container.addView(remoteSurfaceView);

                updateInfo();
            });

            // Setup remote video to render
            agoraEngine.setupRemoteVideoEx(new VideoCanvas(remoteSurfaceView,
                    VideoCanvas.RENDER_MODE_HIDDEN, uid), rtcSecondConnection);
            remoteSurfaceView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);

            channel2Users.remove(uid);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateInfo();
                }
            });
        }
    };


    public void leaveChannel(View view) {
        if (!isJoined) {
            showMessage("Join a channel first");
        } else {
            agoraEngine.leaveChannel();
            showMessage("You left the channel");
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView.setVisibility(View.GONE);
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView.setVisibility(View.GONE);
            isJoined = false;
        }
        audienceRole.setEnabled(true); // Enable the switch

        channel1Users.clear();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateInfo();
            }
        });
    }



    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        // Listen for the remote host joining the channel to get the uid of the host.
        public void onUserJoined(int uid, int elapsed) {
            showMessage("Remote user joined " + uid);

            channel1Users.add(uid);

            if (!audienceRole.isChecked()) return;
            // Set the remote video view
            runOnUiThread(() -> {
                setupRemoteVideo(uid,findViewById(R.id.local_video_view_container));
                updateInfo();
            });
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            isJoined = true;

            channel1Users.add(uid);

            showMessage("Joined Channel " + channel);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    secondChannelButton.setEnabled(true);
                    updateInfo();
                }
            });
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);

            channel1Users.add(stats.users);

            isJoined = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateInfo();
                }
            });

        }

        @Override
        public void onUserOffline(int uid, int reason) {
            showMessage("Remote user offline " + uid + " " + reason);

            channel1Users.remove(uid);

            runOnUiThread(() -> {
                if (remoteSurfaceView != null) remoteSurfaceView.setVisibility(View.GONE);

                updateInfo();
            });
        }
    };

    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        remoteSurfaceView = new SurfaceView(getBaseContext());
        remoteSurfaceView.setZOrderMediaOverlay(true);
        container.addView(remoteSurfaceView);
        agoraEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
        // Display RemoteSurfaceView.
        remoteSurfaceView.setVisibility(View.VISIBLE);
    }

    private void setupLocalVideo() {
        FrameLayout container = findViewById(R.id.local_video_view_container);
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = new SurfaceView(getBaseContext());
        container.addView(localSurfaceView);
        // Call setupLocalVideo with a VideoCanvas having uid set to 0.
        agoraEngine.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    public void joinChannel(View view) {
        if (checkSelfPermission()) {
            ChannelMediaOptions options = new ChannelMediaOptions();
            // For Live Streaming, set the channel profile as LIVE_BROADCASTING.
            options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
            // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
            if (audienceRole.isChecked()) { //Audience
                options.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE;
            } else { //Host
                options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
                // Display LocalSurfaceView.
                setupLocalVideo();
                localSurfaceView.setVisibility(View.VISIBLE);
                // Start local preview.
                agoraEngine.startPreview();
            }
            audienceRole.setEnabled(false); // Disable the switch
            // Join the channel with a temp token.
            // You need to specify the user ID yourself, and ensure that it is unique in the channel.
            agoraEngine.joinChannel(token, channelName, uid, options);
        } else {
            Toast.makeText(getApplicationContext(), "Permissions was not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRemoteVideo(int uid, FrameLayout container) {
        remoteSurfaceView = new SurfaceView(getBaseContext());
        remoteSurfaceView.setZOrderMediaOverlay(true);
        container.addView(remoteSurfaceView);
        agoraEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
        remoteSurfaceView.setVisibility(View.VISIBLE);
    }



    private boolean checkSelfPermission() {
        if (ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) !=  PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) !=  PackageManager.PERMISSION_GRANTED)
        {
            return false;
        }
        return true;
    }

    void showMessage(String message) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

}