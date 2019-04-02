package com.example.anonai;


import android.content.Intent;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.wonderkiln.camerakit.CameraView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class Processing extends AppCompatActivity {
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    private CameraView cameraView;
    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textViewResult = (TextView) findViewById(R.id.textViewResult);
        textViewResult.setMovementMethod(new ScrollingMovementMethod());


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
        } catch (Exception e) {
            System.out.println("Exception= " + e);
        }
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int duration_millisec = Integer.parseInt(duration); //duration in millisec
        int duration_second = duration_millisec / 1000;  //millisec to sec.
        int frames_per_second = 1;  //no. of frames want to retrieve per second
        int numeroFrameCaptured = frames_per_second * duration_second;
        for (int i = 0; i < numeroFrameCaptured; i++) {
            //setting time position at which you want to retrieve frames
            frameList.add(retriever.getFrameAtTime(5000 * i));
            System.out.println("dodal");
        }

        initTensorFlowAndLoadModel();

        // trenutno printa seznam zaznanih objektov in verjetnosti, včasih se sesuje

        for (int i = 0; i < numeroFrameCaptured-1; i++) {
            Bitmap bitmap = frameList.get(i);
            Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
            System.out.println(bitmap1);

            try {List<Classifier.Recognition> results = classifier.recognizeImage(bitmap1);
                System.out.println(results.toString());
                textViewResult.setText(results.toString());
             }
            catch (Exception e){
                System.out.println("problem" + e);
            }


        }


        Intent intent2 = new Intent(Processing.this, VideoPlay.class);
        intent2.putExtra("videoURI", contentURI);
        startActivity(intent2);



    }


/*        @Override
        protected void onResume() {
            super.onResume();
            cameraView.start();
        }

        @Override
        protected void onPause() {
            cameraView.stop();
            super.onPause();
        }*/

        @Override
        protected void onDestroy() {
            super.onDestroy();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    classifier.close();
                }
            });
        }

        private void initTensorFlowAndLoadModel() {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        classifier = TensorFlowImageClassifier.create(
                                getAssets(),
                                MODEL_FILE,
                                LABEL_FILE,
                                INPUT_SIZE,
                                IMAGE_MEAN,
                                IMAGE_STD,
                                INPUT_NAME,
                                OUTPUT_NAME);
                       // makeButtonVisible();
                    } catch (final Exception e) {
                        throw new RuntimeException("Error initializing TensorFlow!", e);
                    }
                }
            });
        }

/*        private void makeButtonVisible() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnDetectObject.setVisibility(View.VISIBLE);
                }
            });
        }*/

}
