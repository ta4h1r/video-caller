package com.jb.androidwebrtc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.beans.OperationResult;
import com.sanbot.opensdk.function.beans.StreamOption;
import com.sanbot.opensdk.function.beans.headmotion.DRelativeAngleHeadMotion;
import com.sanbot.opensdk.function.beans.headmotion.RelativeAngleHeadMotion;
import com.sanbot.opensdk.function.beans.wheelmotion.DistanceWheelMotion;
import com.sanbot.opensdk.function.beans.wheelmotion.NoAngleWheelMotion;
import com.sanbot.opensdk.function.beans.wheelmotion.RelativeAngleWheelMotion;
import com.sanbot.opensdk.function.unit.HDCameraManager;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.ModularMotionManager;
import com.sanbot.opensdk.function.unit.ProjectorManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;
import com.sanbot.opensdk.function.unit.interfaces.media.MediaStreamListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CapturerObserver;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TelepresenceActivity extends TopBaseActivity implements SurfaceHolder.Callback, TextureView.SurfaceTextureListener, VideoSink {

    private final static String TAG = "MainActivity";

    private final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101;
    private final int MY_PERMISSIONS_REQUEST = 102;

    private static final String VIDEO_TRACK_ID = "video1";
    private static final String AUDIO_TRACK_ID = "audio1";
    private static final String LOCAL_STREAM_ID = "stream1";

    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private TextureView localVideoBuffer;
    private EglBase rootEglBase;

    private PeerConnectionFactory peerConnectionFactory;
    private VideoSource localVideoSource;
    private MediaStream localMediaStream;
    private PeerConnection peerConnection;

    private boolean usePadCamera = true;
    private String roomId = "room3";
    private boolean createRoom = false;
    private boolean joinRoom = false;

    private String uri = "turn:numb.viagenie.ca";
    private String usr = "******************";
    private String pass = "***************";

    private String uri2 = "****************";
    private String usr2 = "****************";
    private String pass2 = "****************";
    private String uri3 = "*****************";

    public FirebaseFirestore db;
    public DocumentReference roomRef;
    public CollectionReference callerCandidatesCollection;
    public CollectionReference calleeCandidatesCollection;

    private HDCameraManager hdCameraManager;
    private static int type = 0;

    private static VisionMediaDecoder mediaDecoder;
    private static RtcMediaDecoder rtcDecoder;

    private List<Integer> handleList = new ArrayList<>();
    private org.webrtc.CapturerObserver capturerObs;
    private Thread captureThread;
    private SurfaceTextureHelper surTextureHelper;

    private VideoCapturer vc;

    private SurfaceTexture st;
    private Surface datSurfGo;

    private TextView tvRoomName;
    private TextView tvVol;

    private Thread videoThread;

    private boolean isElf = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(TelepresenceActivity.class);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telepresence);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // The screen is always on
        getSupportActionBar().hide();                                           // Hide the action bar at the top

        askForPermissions();

        mediaDecoder = new VisionMediaDecoder();
        rtcDecoder = new RtcMediaDecoder();

        // Retrieve parameters from parent activity
        usePadCamera = getIntent().getBooleanExtra("USE_PAD_CAM", true);
        isElf = getIntent().getBooleanExtra("IS_ELF", true);
        roomId = getIntent().getStringExtra("ROOM_NAME");

        tvRoomName = findViewById(R.id.tv_room_name);
        tvRoomName.setText("Current room name: " + roomId);
        tvVol = findViewById(R.id.tv_vol);

        videoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                initVideoCallControls();
            }
        });

        FirebaseApp.initializeApp(getApplicationContext());

        videoThread.start();
    }
    public void askForPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST);
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    /**--------- WebRTC video call setup ----------*/
    private void initVideoCallControls() {
        // UI elements
        findViewById(R.id.btnCreateRoom).setEnabled(true);
        findViewById(R.id.btnJoinRoom).setEnabled(true);
        findViewById(R.id.btnHangUp).setEnabled(false);

        // Configure audio
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        // Configure camera
        initViews();

        // Create PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

        // Check that the peerConnectionFactory config is valid
        if(0 == peerConnectionFactory.getNativePeerConnectionFactory()) {
            Log.e("oops", "Illegal PeerConnection object. Something wrong in construction of native peerConnection.");
        }

        if (usePadCamera) {
            // Create video capturer
            vc = createVideoGrabber();
            if (vc != null) {
                // Create and render local video track
                SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
                localVideoSource = peerConnectionFactory.createVideoSource(vc.isScreencast());
                vc.initialize(surfaceTextureHelper, this, localVideoSource.getCapturerObserver());
                vc.startCapture(/*Video resolution width*/640, /*Video resolution height*/480, /*FPS*/30);
                VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);
                localVideoTrack.setEnabled(true);
                localVideoTrack.addSink(this.localVideoView);

                // Create local audio track
                AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
                AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
                localAudioTrack.setEnabled(true);

                // Add tracks to media stream
                localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID);
                localMediaStream.addTrack(localVideoTrack);
                localMediaStream.addTrack(localAudioTrack);
            }
        } else {
            hdCameraManager = (HDCameraManager) getUnitManager(FuncConstant.HDCAMERA_MANAGER);    // The source of the custom video feed
            vc = new VideoCapturer() {
                @Override
                public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
                    surTextureHelper = surfaceTextureHelper;
                    capturerObs = capturerObserver;
                    st = surTextureHelper.getSurfaceTexture();
                }
                @Override
                public void startCapture(int width, int height, int fps) {
                    Log.i("Tai", "VideoCapturer: startCapture");

                    mediaDecoder.setVideoHeight(height);
                    mediaDecoder.setVideoWidth(width);
                    rtcDecoder.setVideoHeight(height);
                    rtcDecoder.setVideoWidth(width);

                    captureThread = new Thread(() -> {
//                        capturerObs.onCapturerStarted(true);
                        initListener();
                    });
                    captureThread.start();
                }
                @Override
                public void stopCapture() throws InterruptedException {
                    captureThread.interrupt();
                }
                @Override
                public void changeCaptureFormat(int i, int i1, int i2) {

                }
                @Override
                public void dispose() {

                }
                @Override
                public boolean isScreencast() {
                    return false;
                }
            };
            SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext(), true);
            VideoSource localTestSource = peerConnectionFactory.createVideoSource(vc.isScreencast());
            vc.initialize(surfaceTextureHelper, this, localTestSource.getCapturerObserver());

            int width = 640;
            int height = 480;
            surTextureHelper.setTextureSize(width, height);
            datSurfGo = new Surface(st);
            rtcDecoder.setSurface(datSurfGo);
            surTextureHelper.startListening(this);
            vc.startCapture(width, height, /*fps*/30);

            VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localTestSource);
            localVideoTrack.setEnabled(true);

            // Create local audio track
            AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
            AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
            localAudioTrack.setEnabled(true);

            // Add tracks to media stream
            localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID);
            localMediaStream.addTrack(localVideoTrack);
            localMediaStream.addTrack(localAudioTrack);
        }

    }
    private void initViews() {
        localVideoView = findViewById(R.id.local_gl_surface_view);
        localVideoBuffer = findViewById(R.id.local_gl_surface_buffer);
        remoteVideoView = findViewById(R.id.remote_gl_surface_view);

        localVideoView.setMirror(true);
        remoteVideoView.setMirror(false);

        // Create appropriate video renderers
        rootEglBase = EglBase.create();
        if (usePadCamera) {
            // Pad camera feed
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    localVideoView.init(rootEglBase.getEglBaseContext(), null);
                    localVideoView.setZOrderMediaOverlay(true);
                }
            });
        } else {
            // HD camera feed
            localVideoView.getHolder().addCallback(this);
            localVideoBuffer.setSurfaceTextureListener(this);
        }
        // Remote feed
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
                remoteVideoView.setZOrderMediaOverlay(true);
            }
        });
    }
    private int mWidth, mHeight;
    private void initListener() {
        hdCameraManager.setMediaListener(new MediaStreamListener() {
            @Override
            public void getVideoStream(int handle, byte[] bytes, int width, int height) {
                // capture the start time
                long startTime = System.nanoTime();


                if (mediaDecoder != null) {
                    if (width != mWidth || height != mHeight) {
                        rtcDecoder.onCreateCodec(width, height);
                        mediaDecoder.onCreateCodec(width, height);
                        mWidth = width;
                        mHeight = height;
                    }
                    mediaDecoder.drawVideoSample(ByteBuffer.wrap(bytes));
                    rtcDecoder.drawVideoSample(ByteBuffer.wrap(bytes));
                }


                // capture the method execution end time
                long endTime = System.nanoTime();
                // dump the execution time out
                Log.d("Time", "TIME: getVideoStream: " + String.valueOf((endTime -
                        startTime)/1000000));
            }

            @Override
            public void getAudioStream(int i, byte[] bytes) {
//                Log.i(TAG, "getAudioStream: Audio data:" + bytes.length);
            }
        });
    }
    public VideoCapturer createVideoGrabber() {
        VideoCapturer videoCapturer;
        videoCapturer = createCameraGrabber(new Camera1Enumerator(false));
        return videoCapturer;
    }
    public VideoCapturer createCameraGrabber(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    public void onCreateRoom(View view) {
        // UI changes
        findViewById(R.id.btnCreateRoom).setEnabled(false);
        findViewById(R.id.btnJoinRoom).setEnabled(true);
        findViewById(R.id.btnHangUp).setEnabled(true);

        createRoom = true;

        // Database references
        db = FirebaseFirestore.getInstance();
        roomRef = db.collection("rooms").document(roomId);

        // ICE servers list TODO: Handle deprecation with .builder
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(new PeerConnection.IceServer(uri, usr, pass));
//        iceServers.add(new PeerConnection.IceServer(uri2, usr2, pass2));
//        iceServers.add(new PeerConnection.IceServer(uri3, usr2, pass2));

        // Create peer connection, add local stream, create new ICE candidates, create offer/room
        peerConnection = peerConnectionFactory.createPeerConnection(
                iceServers,
                new MediaConstraints(),
                peerConnectionObserver);
        peerConnection.addStream(localMediaStream);
        peerConnection.createOffer(sdpObserver, new MediaConstraints());

        // Listen for remote session description
        roomRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "Listen failed.", e);
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
//                    Log.d("Tai", "current data: " + documentSnapshot.getData());
                    Log.i("Tai", "Got data snapshot");
                    if (documentSnapshot.contains("answer")) {
                        Map<String, Object> obj = new HashMap<>();
                        obj.put("answer", documentSnapshot.get("answer"));
                        Collection vals = obj.values();
                        JSONArray valsArr = new JSONArray(vals);
                        try {
                            JSONObject objec = valsArr.getJSONObject(0);
                            Log.i("Tai", "Got remote description");
                            SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER, objec.getString("sdp"));
                            peerConnection.setRemoteDescription(sdpObserver, sdp);
                            if (peerConnection.getRemoteDescription() != null) {
                                Log.i("Tai", "Set remote description success");
                            }
                        } catch (JSONException ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    Log.d("Tai", "Current data: null");
                }
            }
        });

        // Listen for remote ICE candidates
        roomRef.collection("calleeCandidates").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    List<DocumentChange> q = queryDocumentSnapshots.getDocumentChanges();
                    Map<String, Object> obj = new HashMap<>();
                    for (int i = 0; i < q.size(); i++) {
                        obj.put("candidates", q.get(i).getDocument().getData());
                        Log.d("Tai", "Found new remote candidate: " + q.get(i).getDocument().getData());
                    }
                    Collection vals = obj.values();
                    JSONArray valsArr = new JSONArray(vals);
                    try {
                        JSONObject objec = valsArr.getJSONObject(0);
//                        Log.i("Tai", String.valueOf(objec));
                        peerConnection.addIceCandidate(new IceCandidate(objec.getString("sdpMid"),
                                objec.getInt("sdpMLineIndex"),
                                objec.getString("candidate")));
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    Log.d("Tai", "Current data: null");
                }
            }
        });
    }
    public void onJoinRoom(View view) {
        // UI changes
        findViewById(R.id.btnCreateRoom).setEnabled(false);
        findViewById(R.id.btnJoinRoom).setEnabled(false);
        findViewById(R.id.btnHangUp).setEnabled(true);
        joinRoom = true;
        Log.i("Taibai", roomId);
        joinRoomById(roomId);
    }
    private void joinRoomById(String roomId) {
        // Database references
        db = FirebaseFirestore.getInstance();
        roomRef = db.collection("rooms").document(roomId);

        // Join room if it exists
        Task<DocumentSnapshot> roomSnapshot = roomRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Log.i("Tai", "Got room: " + documentSnapshot.exists());
                        if (documentSnapshot.exists()) {
                            // ICE servers list
                            ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
                            iceServers.add(new PeerConnection.IceServer(uri, usr, pass));
                            iceServers.add(new PeerConnection.IceServer(uri2, usr2, pass2));
                            iceServers.add(new PeerConnection.IceServer(uri3, usr2, pass2));

                            // Create peer connection, add local stream
                            peerConnection = peerConnectionFactory.createPeerConnection(
                                    iceServers,
                                    new MediaConstraints(),
                                    peerConnectionObserver);
                            peerConnection.addStream(localMediaStream);

                            // Get remote session description (offer)
                            if (documentSnapshot != null && documentSnapshot.exists()) {
//                          Log.d("Tai", "current data: " + documentSnapshot.getData());
                                Log.i("Tai", "Got data snapshot");
                                if (documentSnapshot.contains("offer")) {
                                    Map<String, Object> obj = new HashMap<>();
                                    obj.put("offer", documentSnapshot.get("offer"));
                                    Collection vals = obj.values();
                                    JSONArray valsArr = new JSONArray(vals);
                                    try {
                                        JSONObject objec = valsArr.getJSONObject(0);
                                        Log.i("Tai", "Got remote description");
                                        SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, objec.getString("sdp"));
                                        peerConnection.setRemoteDescription(sdpObserver, sdp);
                                        if (peerConnection.getRemoteDescription() != null) {
                                            Log.i("Tai", "Set remote description success");
                                        }
                                    } catch (JSONException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            } else {
                                Log.d("Tai", "Current data: null");
                            }

                            peerConnection.createAnswer(sdpObserver, new MediaConstraints());  // Create new ICE candidates in response to offer

                            // Listen for remote ICE candidates
                            roomRef.collection("callerCandidates").addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                    if (e != null) {
                                        Log.w("Tai", "Listen failed.", e);
                                        return;
                                    }
                                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                                        List<DocumentChange> q = queryDocumentSnapshots.getDocumentChanges();
                                        Map<String, Object> obj = new HashMap<>();
                                        for (int i = 0; i < q.size(); i++) {
                                            obj.put("candidates", q.get(i).getDocument().getData());
                                            Log.d("Tai", "Found new remote candidate: " + q.get(i).getDocument().getData());
                                        }
                                        Collection vals = obj.values();
                                        JSONArray valsArr = new JSONArray(vals);
                                        try {
                                            JSONObject objec = valsArr.getJSONObject(0);
//                        Log.i("Tai", String.valueOf(objec));
                                            peerConnection.addIceCandidate(new IceCandidate(objec.getString("sdpMid"),
                                                    objec.getInt("sdpMLineIndex"),
                                                    objec.getString("candidate")));
                                        } catch (JSONException ex) {
                                            ex.printStackTrace();
                                        }
                                    } else {
                                        Log.d("Tai", "Current data: null");
                                    }
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Failed to join room", Toast.LENGTH_LONG).show();
                                    findViewById(R.id.btnCreateRoom).setEnabled(true);
                                    findViewById(R.id.btnJoinRoom).setEnabled(true);
                                    findViewById(R.id.btnHangUp).setEnabled(false);

                                }
                            });
                        }
                    }
                });
    }
    public void onHangUp(View view) {
        // TODO: Kill remote screen
        // UI changes
        findViewById(R.id.btnCreateRoom).setEnabled(true);
        findViewById(R.id.btnJoinRoom).setEnabled(true);
        findViewById(R.id.btnHangUp).setEnabled(false);

        joinRoom = false;
        createRoom = false;

        // Close peer connection and delete the room
        if (peerConnection != null) {
            peerConnection.close();

            // Delete the room
            db = FirebaseFirestore.getInstance();
            roomRef = db.collection("rooms").document(roomId);
            roomRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    roomRef.delete();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Deleted room: " + roomId, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        }

        // Kill resources and restart
//        remoteVideoView.release();
//        localVideoView.release();
//        st.release();
//        datSurfGo.release();
//        rtcDecoder.stopDecoding();
//        mediaDecoder.stopDecoding();
//        mWidth = 0;
//        mHeight = 0;
//        releaseInstance();
//        recreate();
//        videoThread.interrupt();
//        localVideoView.release();
        finish();
    }
    public MediaStream mMediaStream;
    public int vol = 2;
    public void onVolUp(View view) {
        vol +=1;
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvVol.setText(String.valueOf(vol));
                }
            });
            mMediaStream.audioTracks.get(0).setVolume(vol);
        } catch (NullPointerException e) {
            Log.e("Tai", TAG + ": onVolUp: NullPointerException: " + e);
        }
    }
    public void onVolDown(View view) {
        vol -= 1;
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvVol.setText(String.valueOf(vol));
                }
            });
            mMediaStream.audioTracks.get(0).setVolume(vol);
        } catch (NullPointerException e) {
            Log.e("Tai", TAG + ": onVolDown: NullPointerException: " + e);
        }
    }

    SdpObserver sdpObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.i("Tai", "sdpObserver:onCreateSuccess, joinRoom = " + joinRoom + ", createRoom = " + createRoom);
            if (createRoom) {
                peerConnection.setLocalDescription(sdpObserver, sessionDescription);
                Log.i("Tai", "Set local description success");

                Map<String, Object> roomWithOffer = new HashMap<>();
                Map<String, Object> obj = new HashMap<>();
                obj.put("type", sessionDescription.type.toString().toLowerCase());
                obj.put("sdp", sessionDescription.description);
                roomWithOffer.put("offer", obj);
                roomRef.set(roomWithOffer);

                Log.i("Tai", "New room created with SDP offer. Room ID: " + roomRef.getId());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Created room: " + roomRef.getId(), Toast.LENGTH_LONG).show();
                    }
                });
            } else if (joinRoom) {
                peerConnection.setLocalDescription(sdpObserver, sessionDescription);
                Log.i("Tai", "Set local description success");

                Map<String, Object> roomWithAnswer = new HashMap<>();
                Map<String, Object> obj = new HashMap<>();
                obj.put("type", sessionDescription.type.toString().toLowerCase());
                obj.put("sdp", sessionDescription.description);
                roomWithAnswer.put("answer", obj);
                roomRef.update(roomWithAnswer);

                Log.i("Tai", "Attached room with SDP answer. Room ID: " + roomRef.getId());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Joined room: " + roomRef.getId(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Log.i("Tai", "sdpObserver: Nothing is coming");
            }
        }
        @Override
        public void onSetSuccess() {
            Log.i("Tai", "sdpObserver: onSetSuccess");

        }
        @Override
        public void onCreateFailure(String s) {
            Log.i("Tai", "sdpObserver: onCreateFailure: " + s);
        }
        @Override
        public void onSetFailure(String s) {
            Log.i("Tai", "sdpObserver: onSetFailure: " + s);
        }
    };
    final PeerConnection.Observer peerConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d("Tai", "onSignalingChange:" + signalingState.toString());
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d("Tai", "onIceConnectionChange:" + iceConnectionState.toString());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d("Tai", "onIceConnectionReceivingChange:" + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d("Tai", "onGatheringChange:" + iceGatheringState.toString());
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.i("Tai", "onIceCandidate: " + iceCandidate.toString());

            if (createRoom) {
                // Add a new document with a generated ID
                callerCandidatesCollection = roomRef.collection("callerCandidates");
                Map<String, Object> obj = new HashMap<>();
                obj.put("candidate", iceCandidate.sdp);
                obj.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                obj.put("sdpMid", iceCandidate.sdpMid);
                callerCandidatesCollection.add(obj)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("Tai", "callerCandidate added with ID: " + documentReference.getId());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("Tai", "Error adding document", e);
                            }
                        });
            } else if (joinRoom) {
                // Add a new document with a generated ID
                calleeCandidatesCollection = roomRef.collection("calleeCandidates");
                Map<String, Object> obj = new HashMap<>();
                obj.put("candidate", iceCandidate.sdp);
                obj.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                obj.put("sdpMid", iceCandidate.sdpMid);
                calleeCandidatesCollection.add(obj)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("Tai", "calleeCandidate added with ID: " + documentReference.getId());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("Tai", "Error adding document", e);
                            }
                        });
            } else {
                Log.i("Tai", "peerConnectionObserver: Nothing is coming");
//                Log.d("Tai", "joinRoom = " + joinRoom + ", createRoom = " + createRoom);
            }

        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d("Tai", "onIceCandidatesRemoved:" + iceCandidates.toString());
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            mMediaStream = mediaStream;
            if (capturerObs != null) {
                capturerObs.onCapturerStarted(true);            // Fixes startup latency by including it here
            }
            mediaStream.videoTracks.get(0).addSink(remoteVideoView);
            mediaStream.audioTracks.get(0).setVolume(vol);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {
            Log.i("Tai", "Negotiation needed");
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.d("Tai", "onAddTrack: " + mediaStreams.toString());
        }
    };

    /**--------- Activity ----------*/
    // TODO: Kill local and remote GL surface views on hang up and/or destroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        findViewById(R.id.btnHangUp).callOnClick();
//        if (vc != null) {
//            vc.dispose();
//            st.release();
//            datSurfGo.release();
//        }
//        localVideoView.clearImage();
//        remoteVideoView.clearImage();
//
//        localVideoView.release();
//        remoteVideoView.release();
//
//        rootEglBase.releaseSurface();
//        rootEglBase.detachCurrent();
//        rootEglBase.release();
    }
    @Override
    protected void onMainServiceConnected() {

    }

    /**---------- SurfaceHolder.Callback ------------
     * controls surfaces associated with mediaDecoder
     * */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Set parameters and turn on media streaming
        StreamOption streamOption = new StreamOption();
        if (type == 1) {
            streamOption.setChannel(StreamOption.MAIN_STREAM);
        } else {
            streamOption.setChannel(StreamOption.SUB_STREAM);
        }
        streamOption.setDecodType(StreamOption.HARDWARE_DECODE);
        streamOption.setJustIframe(false);
        OperationResult operationResult = hdCameraManager.openStream(streamOption);
        Log.i(TAG, "surfaceCreated: operationResult=" + operationResult.getResult());
        int result = Integer.parseInt(operationResult.getResult());
        if (result != -1) {
            if (handleList == null)  { handleList = new ArrayList<>(); }
            handleList.add(result);
        }
        mediaDecoder.setSurface(holder.getSurface());
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: ");
        // Turn off media streaming
        if (handleList.size() > 0) {
            for (int handle : handleList) {
                Log.i(TAG, "surfaceDestroyed: " + hdCameraManager.closeStream(handle));
            }
        }
        handleList = null;
        mediaDecoder.stopDecoding();
    }

    /**------------ TextureView.SurfaceTextureListener + VideoSink (onFrame) --------------
     * controls surfaces associated with rtcDecoder
     * */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "SurfaceTexture ready (" + width + "x" + height + ")");
//        surTextureHelper.setTextureSize(width, height);
//        datSurfGo = new Surface(st);
//        rtcDecoder.setSurface(datSurfGo);
//        surTextureHelper.startListening(this);
//        vc.startCapture(width, height, /*fps*/30);
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        rtcDecoder.stopDecoding();
        return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // If this is being called, it means that you're not using the SurfaceTextureHelper
        Log.d(TAG, "SurfaceTextureUpdated: " + surface.getTimestamp());
    }

    @Override
    public void onFrame(VideoFrame videoFrame) {
        boolean frameSkip = false;
        if (frameSkip == true) {     //TODO: Make sure that frames are only sent to the observer if the decoder has run at least 2-3 times (put a counter on the mediastream listener, and switch the fremeSkip boolean when appropriate)
            Log.i(TAG, "onFrame: skipped");
        } else {
            Log.i(TAG, "onFrame: sent");
            capturerObs.onFrameCaptured(videoFrame);
//            localVideoView.onFrame(videoFrame);
        }
    }


}
