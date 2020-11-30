package com.example.videoconnect;

import android.Manifest;
import android.content.Intent;
import android.icu.lang.UCharacter;
import android.opengl.GLSurfaceView;
//import android.se.omapi.Session;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.v3.OpentokException;
//import com.opentok.android.v3.Session;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class VideoChatActivity extends AppCompatActivity
        implements Session.SessionListener,
        PublisherKit.PublisherListener
{
    private static String API_Key = "46736332";
    private static String SESSION_ID = "1_MX40NjczNjMzMn5-MTU4OTM4MzkyMjkyMH51MTcrTGJiRlRnajNtMlVkNFNZZlF4Z01-fg";
    private static String TOKEN = "T1==cGFydG5lcl9pZD00NjczNjMzMiZzaWc9Y2ZkZmM2MDFhMDJhMTVhNGZjMzFiYWIwNWRiZTRkYmY1YTAxZDE0MzpzZXNzaW9uX2lkPTFfTVg0ME5qY3pOak16TW41LU1UVTRPVE00TXpreU1qa3lNSDUxTVRjclRHSmlSbFJuYWpOdE1sVmtORk5aWmxGNFowMS1mZyZjcmVhdGVfdGltZT0xNTg5Mzg0MDI5Jm5vbmNlPTAuNDg5NzI1NDE1OTczMjU5NiZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNTkxOTc2MDI2JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private static final String LOG_TAG = VideoChatActivity.class.getSimpleName();
    private static final int RC_VIDEO_APP_PERM = 124;

    private FrameLayout mPublisherViewController;
    private FrameLayout mSubcriberViewController;
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private ImageView closeVideoChatBtn;
    private DatabaseReference usersRef;
    private String userID = "";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        closeVideoChatBtn = findViewById(R.id.close_video_chat_btn);
        closeVideoChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usersRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.child(userID).hasChild("Ringing"))
                        {
                            usersRef.child(userID).child("Ringing").removeValue();

                            if (mPublisher != null)
                            {
                                mPublisher.destroy();
                            }

                            if (mSubscriber != null)
                            {
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                        }

                        if (dataSnapshot.child(userID).hasChild("Calling"))
                        {
                            usersRef.child(userID).child("Calling").removeValue();

                            if (mPublisher != null)
                            {
                                mPublisher.destroy();
                            }

                            if (mSubscriber != null)
                            {
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                        }
                        else
                            {
                                if (mPublisher != null)
                                {
                                    mPublisher.destroy();
                                }

                                if (mSubscriber != null)
                                {
                                    mSubscriber.destroy();
                                }

                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                            }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

        requestPermission();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, VideoChatActivity.this);
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermission()
    {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        if (EasyPermissions.hasPermissions(this, perms)) {
            mPublisherViewController = findViewById(R.id.publisher_container);
            mSubcriberViewController = findViewById(R.id.subscriber_container);


            //1. intitialize and connect to the session

            mSession = new Session.Builder(this, API_Key, SESSION_ID).build();
            mSession.setSessionListener(VideoChatActivity.this);
            mSession.connect(TOKEN);
        }

        else
        {
            EasyPermissions.requestPermissions(this, "Hey this app needs Mic and Camera permission, Please allow.", RC_VIDEO_APP_PERM, perms);
        }

    }

    //2. Publishing a stream to the session
    @Override
    public void onConnected(Session session)
    {
        Log.i(LOG_TAG, "Session Connected");

        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoChatActivity.this);

        mPublisherViewController.addView(mPublisher.getView());

        if (mPublisher.getView() instanceof GLSurfaceView)
        {
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session)
    {
        Log.i(LOG_TAG, "Stream Disconnected");
    }

    //3.Subscribing to the streams

    @Override
    public void onStreamReceived(Session session, Stream stream)
    {
        Log.i(LOG_TAG, "Stream Received");
        if (mSubscriber == null)
        {
            mSubscriber = new Subscriber.Builder(this, stream).build();
            mSession.subscribe(mSubscriber);
            mSubcriberViewController.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream)
    {
        Log.i(LOG_TAG, "Stream Dropped");

        if (mSubscriber!= null)
        {
            mSubscriber = null;
            mSubcriberViewController.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError)
    {
        Log.i(LOG_TAG, "Stream Error");
    }





    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}

