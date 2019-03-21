package com.example.anonai;


import android.content.Intent;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.util.ArrayList;

public class Processing extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        Intent intent = getIntent();
        Uri contentURI = intent.getParcelableExtra("videoURI");

        // Koda za zajem slik z videa

        //ArrayList<Bitmap> frameList;
        ArrayList<Bitmap> frameList = new ArrayList<>();

        // MediaMetadataRetriever class is used to retrieve meta data from methods. *//*
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        

        try {
            //path of the video of which you want frames
            retriever.setDataSource(this, contentURI);
            System.out.println("neki je");
        }catch (Exception e) {
            System.out.println("Exception= "+e);
        }
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int duration_millisec = Integer.parseInt(duration); //duration in millisec
        int duration_second = duration_millisec / 1000;  //millisec to sec.
        int frames_per_second = 2;  //no. of frames want to retrieve per second
        int numeroFrameCaptured = frames_per_second * duration_second;
        for (int i = 0; i < numeroFrameCaptured; i++)
        {
            //setting time position at which you want to retrieve frames
            frameList.add(retriever.getFrameAtTime(5000*i));
            System.out.println("dodal");
        }




        Intent intent2 = new Intent(Processing.this, VideoPlay.class);
        intent2.putExtra("videoURI", contentURI);
        startActivity(intent2);


    }
}
