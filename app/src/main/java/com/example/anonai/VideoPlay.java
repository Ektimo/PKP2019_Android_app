package com.example.anonai;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.widget.VideoView;
import android.net.Uri;

import java.io.File;

public class VideoPlay extends AppCompatActivity {

    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_play);

        videoView = findViewById(R.id.fullscreen_video);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);


        getSupportActionBar().hide();

        Intent intent = getIntent();
        Uri contentURI = intent.getParcelableExtra("videoURI1");

        String fullVideoName = contentURI.toString();
        String videoName =

        File mydir = this.getDir("Videos", this.MODE_PRIVATE);

        File fileWithinMyDir = new File(mydir, videoName);

        fileWithinMyDir.setReadable(true, false);

        String videoResource = fileWithinMyDir.getPath();

        Uri intentUri = Uri.fromFile(new File(videoResource));

        videoView.setVideoURI(contentURI);
        videoView.requestFocus();
        videoView.start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent i = new Intent(VideoPlay.this, MainActivity.class);
                startActivity(i);
            }
        }, 10000);

    }
}