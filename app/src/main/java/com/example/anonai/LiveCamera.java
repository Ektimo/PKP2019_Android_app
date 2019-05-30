package com.example.anonai;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import android.widget.VideoView;

import com.wonderkiln.camerakit.CameraView;

public class LiveCamera extends AppCompatActivity {

    private CameraView cameraView;
    private TextView textViewResult;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_camera);


        Intent intent = getIntent();
    }
}
