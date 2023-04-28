package com.multichannelstreaming;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
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
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMediaOperationProgress;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.SendMessageOptions;

import android.widget.Button;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    // Fill the App ID of your project generated on Agora Console.
    private final String appId = "9b82a4a719e24fe280fe652fe06f2f6b";
    // Fill the channel name.
    private String channelName = "channel1";
    // Fill the temp token generated on Agora Console.
    private String token = "007eJxTYFgycwqrw79tgVv7/z8/oHeHOzdRaYand/UdR4OUf/M823sUGCyTLIwSTRLNDS1TjUzSUo0sDNJSzUyN0lINzNKM0sySrjh7pzQEMjJ0vORgYmRgZGABYhCfCUwyg0kWMMnBkJyRmJeXmmPIwAAAeEgkEw==";
    // An integer that identifies the local user.
    private int currentUid;
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
    private int secondChannelUid;
    private String secondChannelToken = "007eJxTYLj+2Y85plbYfPIsN4UzljGMbBM6Ejaw/W473efucvppuo8Cg2WShVGiSaK5oWWqkUlaqpGFQVqqmalRWqqBWZpRmlmSiYt3SkMgI8Pe//aMjAyMDCxADOIzgUlmMMkCJjkYkjMS8/JSc4wYGAB29CHL";
    private boolean isSecondChannelJoined = false; // Track connection state of the second channel



    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
    };
    private ListView usersListView;
    private static final String TAG = "MainActivity";
    private UsersAdapter adapter;

    private RtmClient mRtmClient;
    // <Vg k="MESS" /> channel instance
    private RtmChannel mRtmChannel;
    private String rtmTokenString;


    static class User {
        public String name;
        public boolean isHost;
        public boolean isMuted;

        public User(String name, boolean isHost, boolean isMuted){
            this.name = name;
            this.isHost = isHost;
            this.isMuted = isMuted;
        }
    }

    public class UsersAdapter extends ArrayAdapter<User> {

        private ArrayList<User> users;

        public UsersAdapter(Context context, ArrayList<User> users) {
            super(context, 0, users);
            this.users = users;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            User user = users.get(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.user_layout, parent, false);
            }
            // Lookup view for data population
            TextView tvName = (TextView) convertView.findViewById(R.id.username);
            CheckBox checkHost = (CheckBox) convertView.findViewById(R.id.makehost);
            CheckBox muteUnmute = (CheckBox) convertView.findViewById(R.id.muteunmute);
            Button greet = (Button) convertView.findViewById(R.id.greetwithhello);
            tvName.setText(user.name);
            checkHost.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    Log.d(TAG, "checkHost onCheckedChanged() called with: compoundButton = [" + compoundButton + "], b = [" + b + "]");
                }
            });

            muteUnmute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    Log.d(TAG, "muteUnmute onCheckedChanged() called with: compoundButton = [" + compoundButton + "], b = [" + b + "]");
                }
            });

            greet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "greet onClick() called with: view = [" + view + "]");

                    final RtmMessage message = mRtmClient.createMessage();
                    message.setText("Hello from " + currentUid);

                    SendMessageOptions option = new SendMessageOptions();
                    option.enableOfflineMessaging = true;
                    mRtmChannel.sendMessage(message, new ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "onSuccess() called with: unused = [" + unused + "]");
                            Toast.makeText(MainActivity.this, "Message Sent Successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(ErrorInfo errorInfo) {
                            Log.d(TAG, "onFailure() called with: errorInfo = [" + errorInfo + "]");
                        }
                    });
                }
            });
            // Return the completed view to render on screen
            return convertView;
        }


        public void addUsers(ArrayList<User> newUsers) {
            users.clear();
            users.addAll(newUsers);
            notifyDataSetChanged();
        }
    }

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

        ArrayList<User> arrayOfUsers = new ArrayList<User>();
        // Create the adapter to convert the array to views
        adapter = new UsersAdapter(this, arrayOfUsers);

        usersListView = (ListView) findViewById(R.id.list);

        Button sendMessageToPeerButton = (Button) findViewById(R.id.sendmessagetopeer);
        sendMessageToPeerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText taskEditText = new EditText(MainActivity.this);
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Enter the peer UID")
                        .setView(taskEditText)
                        .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String peerId = taskEditText.getText().toString();

                                final RtmMessage message = mRtmClient.createMessage();
                                message.setText("Hello from " + currentUid);

                                SendMessageOptions option = new SendMessageOptions();
                                option.enableOfflineMessaging = true;
                                mRtmClient.sendMessageToPeer(peerId, message, option, new ResultCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "sendMessageToPeer onSuccess() called with: unused = [" + unused + "]");
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(MainActivity.this, "Message Sent Successfully", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(ErrorInfo errorInfo) {
                                        Log.d(TAG, "sendMessageToPeer onFailure() called with: errorInfo = [" + errorInfo + "]");
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.show();
            }
        });

        usersListView.setAdapter(adapter);

        initialiseRTMClient();

        final EditText taskEditText = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter UID")
                .setView(taskEditText)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        currentUid = Integer.parseInt(taskEditText.getText().toString());

                        secondChannelUid = currentUid;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView currentUserId = (TextView)findViewById(R.id.currentUserID);
                                currentUserId.setText("CurrentUser : " + String.valueOf(currentUid));
                            }
                        });
                        if(currentUid == 11111){
                            rtmTokenString = "0069b82a4a719e24fe280fe652fe06f2f6bIABSiUQAxX7T1CD8A5gDDIJ1QIpAQAWylMHYQbkEj3Qu8cBx3qAAAAAAEAAJAd5cmYtMZAEA6AOZi0xk";
                        }else if(currentUid == 22222){
                            rtmTokenString = "0069b82a4a719e24fe280fe652fe06f2f6bIAC3U0nSW6mKQL8zJhBmDKn4B2XGV0M5tllOURFMomXG5t4YqUUAAAAAEAAJAd5cE4pMZAEA6AMTikxk";
                        }else if(currentUid == 33333){
                            rtmTokenString = "0069b82a4a719e24fe280fe652fe06f2f6bIAAQfqISjvhszoWFJrRPThCJFXdFLuzuG+Z7bJ78ml2zZes9q68AAAAAEAAJAd5cIYpMZAEA6AMhikxk";
                        }else if(currentUid == 44444){
                            rtmTokenString = "0069b82a4a719e24fe280fe652fe06f2f6bIABVc4MrH77zwQ4g7jBvQYZgBBtsTzzBla/nShzyMDub86PMNlQAAAAAEAAJAd5cNIpMZAEA6AM0ikxk";
                        }

                        loginToRTMClient();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private void initialiseRTMClient() {
        // Initialize the <Vg k="MESS" /> client
        try {
            // Initialize the <Vg k="MESS" /> client
            mRtmClient = RtmClient.createInstance(getBaseContext(), appId,
                    new RtmClientListener() {
                        @Override
                        public void onConnectionStateChanged(int state, int reason) {
                            String text = "Connection state changed to " + state + "Reason: " + reason + "\n";
                            //writeToMessageHistory(text);
                            Log.d(TAG, "onConnectionStateChanged() called with: state = [" + state + "], reason = [" + reason + "]");
                        }

                        @Override
                        public void onTokenExpired() {
                        }

                        @Override
                        public void onPeersOnlineStatusChanged(Map<String, Integer> map) {
                            Log.d(TAG, "onPeersOnlineStatusChanged() called with: map = [" + map + "]");
                        }

                        @Override
                        public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
                            String text = "Message received from " + peerId + " Message: " + rtmMessage.getText() + "\n";
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onImageMessageReceivedFromPeer(RtmImageMessage rtmImageMessage, String s) {

                        }

                        @Override
                        public void onFileMessageReceivedFromPeer(RtmFileMessage rtmFileMessage, String s) {

                        }

                        @Override
                        public void onMediaUploadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {

                        }

                        @Override
                        public void onMediaDownloadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {

                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException("<Vg /> initialization failed!");
        }
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

        logoutFromRTMClient();

        // Destroy the engine in a sub-thread to avoid congestion
        new Thread(() -> {
            RtcEngine.destroy();
            agoraEngine = null;
        }).start();
    }

    private void logoutFromRTMClient() {
        mRtmClient.logout(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(TAG, "logout from RTM onSuccess() called with: unused = [" + unused + "]");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.d(TAG, "logout from RTM onFailure() called with: errorInfo = [" + errorInfo + "]");
            }
        });
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

        ArrayList<User> users = new ArrayList<>();
        for(Integer s: channel1Users){
            users.add(new User(String.valueOf(s), false, false));
        }
        adapter.addUsers(users);
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
            mRtmChannel.leave(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Log.d(TAG, "channel1 left successfully onSuccess() called with: unused = [" + unused + "]");
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    Log.d(TAG, "error leaving channel1 onFailure() called with: errorInfo = [" + errorInfo + "]");
                }
            });
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
            int res = agoraEngine.joinChannel(token, channelName, currentUid, options);
            if (res == 0) {
                mRtmChannel = mRtmClient.createChannel(channelName, new RtmChannelListener() {
                    @Override
                    public void onMemberCountUpdated(int i) {
                        Log.d(TAG, "onMemberCountUpdated() called with: i = [" + i + "]");
                    }

                    @Override
                    public void onAttributesUpdated(List<RtmChannelAttribute> list) {
                        Log.d(TAG, "onAttributesUpdated() called with: list = [" + list + "]");
                    }

                    @Override
                    public void onMessageReceived(RtmMessage rtmMessage, RtmChannelMember rtmChannelMember) {
                        Log.d(TAG, "onMessageReceived() called with: rtmMessage = [" + rtmMessage + "], rtmChannelMember = [" + rtmChannelMember + "]");
                        String text = rtmMessage.getText();
                        String fromUser = rtmChannelMember.getUserId();

                        String message_text = "Message received from " + fromUser + " : " + text + "\n";
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, message_text, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onImageMessageReceived(RtmImageMessage rtmImageMessage, RtmChannelMember rtmChannelMember) {
                        Log.d(TAG, "onImageMessageReceived() called with: rtmImageMessage = [" + rtmImageMessage + "], rtmChannelMember = [" + rtmChannelMember + "]");
                    }

                    @Override
                    public void onFileMessageReceived(RtmFileMessage rtmFileMessage, RtmChannelMember rtmChannelMember) {
                        Log.d(TAG, "onFileMessageReceived() called with: rtmFileMessage = [" + rtmFileMessage + "], rtmChannelMember = [" + rtmChannelMember + "]");
                    }

                    @Override
                    public void onMemberJoined(RtmChannelMember rtmChannelMember) {
                        Log.d(TAG, "onMemberJoined() called with: rtmChannelMember = [" + rtmChannelMember + "]");
                    }

                    @Override
                    public void onMemberLeft(RtmChannelMember rtmChannelMember) {
                        Log.d(TAG, "onMemberLeft() called with: rtmChannelMember = [" + rtmChannelMember + "]");
                    }
                });

                mRtmChannel.join(new ResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "RMTChannel join onSuccess() called with: unused = [" + unused + "]");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                    }

                    @Override
                    public void onFailure(ErrorInfo errorInfo) {
                        Log.d(TAG, "RMTChannel join onFailure() called with: errorInfo = [" + errorInfo + "]");
                    }
                });
            }
        } else {
            Toast.makeText(getApplicationContext(), "Permissions was not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginToRTMClient() {
        mRtmClient.login(rtmTokenString, String.valueOf(currentUid), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(TAG, "RTM Login onSuccess() called with: unused = [" + unused + "]");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.d(TAG, "RTM Login onFailure() called with: errorInfo = [" + errorInfo + "]");
            }
        });
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