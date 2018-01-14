package com.example.david.athletetracker;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;


public class VideoPlay extends AppCompatActivity{


    private VideoView videoView;
    private ProgressBar proBar;
    private MediaController mediaController;
    String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.supportRequestWindowFeature(Window.FEATURE_NO_TITLE); // Para que no aparezca la barra de titulo

        setContentView(R.layout.activity_video_play);


        init();

        videoView.setMediaController(mediaController);
        videoView.setVideoPath(filePath);
        videoView.requestFocus();

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                proBar.setVisibility(View.GONE);
                mp.setPlaybackSpeed(1.0f);
            }
        });

    }

    private void init(){

        filePath = Environment.getExternalStorageDirectory() + "/Atlethe Tracker/" + getIntent().getStringExtra("filename");
        videoView = (VideoView) findViewById(R.id.videoView);
        proBar = (ProgressBar) findViewById(R.id.proBar);
        mediaController = new MediaController(VideoPlay.this);
    }

}
