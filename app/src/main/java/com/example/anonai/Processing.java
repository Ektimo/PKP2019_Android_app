package com.example.anonai;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

//import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.IOException;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//import wseemann.media.FFmpegMediaMetadataRetriever;


public class Processing extends AppCompatActivity {
    public static final int INPUT_SIZE = 320;
    public static final String INPUT_NAME = "input";
    public static final String OUTPUT_NAME = "output";
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String VIDEO_DIRECTORY = "/anonai";
    public static final float MIN_CONFIDENCE = 0.01f;

    public static final String MODEL_FILE = "face.tflite";
    public static final String LABEL_FILE = "file:///android_asset/labels.txt";

    public Classifier classifier;
    public Executor executor = Executors.newSingleThreadExecutor();

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        Uri contentURI = intent.getParcelableExtra("videoURI");
        String fileName = getFileName(contentURI);


        final ArrayList<Bitmap> frameList = new ArrayList<>();

        final MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        final Context context = getApplicationContext();

        //FFmpeg.execute("-i /storage/emulated/0/DCIM/Camera/VID_20190423_124214.mp4 -ss 00:00:03.000 -vframes 1 /storage/emulated/0/DCIM/Camera/thumb.jpg");

        try {
            //path of the video of which you want frames
            retriever.setDataSource(this, contentURI);
        } catch (Exception e) {
            System.out.println("Exception= " + e);
        }

        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        String numberOfFrames = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);
        final int NOF = Integer.parseInt(numberOfFrames);
        final int duration_millisec = Integer.parseInt(duration); //duration in millisec
        final int frames_per_second = 10;  //no. of frames want to retrieve per second
        final int numeroFrameCaptured = Math.min(frames_per_second * (duration_millisec / 1000), NOF);

        FileChannelWrapper out = null;

        initTensorFlowAndLoadModel();

        try {
            File root = new File(Environment.getExternalStorageDirectory() + VIDEO_DIRECTORY);
            File dir = new File(root.getAbsolutePath());

            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(dir, fileName);
            file.setReadable(true, false);
            String path = file.getAbsolutePath();
            out = NIOUtils.writableFileChannel(path);
            // for Android use: AndroidSequenceEncoder
            final AndroidSequenceEncoder encoder = new AndroidSequenceEncoder(out, Rational.R(numeroFrameCaptured, (duration_millisec / 1000)));

            //tfliteOptions.setNumThreads(10);

            Runnable runnable = new Runnable() {

                public void run(){
                    for (int i = 0; i < numeroFrameCaptured; i++) {
                    //for (int i = 1; i < frames_per_second*duration_millisec/1000; i++) {
                        frameList.add(retriever.getFrameAtIndex(i * NOF / numeroFrameCaptured));
                        //long t = i*1000*1000/frames_per_second;
                        //frameList.add(retriever.getScaledFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST,INPUT_SIZE,INPUT_SIZE));
                        System.out.println(i);

                        Bitmap bitmap = frameList.get(i);
                        int oriSizeX = bitmap.getWidth();
                        int oriSizeY = bitmap.getHeight();
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

                        try {
                            List<Classifier.Recognition> results = classifier.recognizeImage(scaledBitmap);
                            System.out.println(results.toString());
                            int numOfRes = results.size();
                            List<Classifier.Recognition> dobriRes = new ArrayList<Classifier.Recognition>();

                            for (int k=0; k<numOfRes; k++){
                                if (results.get(k).getConfidence() > MIN_CONFIDENCE && results.get(k).getConfidence() < 1.1f){
                                    dobriRes.add(results.get(k));
                                } else {
                                    break;
                                }
                            }

                            List<List<Integer>> CordsInt = new ArrayList<List<Integer>>();

                            int numOfDobri = dobriRes.size();
                            if (numOfDobri == 0) {
                                encoder.encodeImage(bitmap);
                            } else {
                                for (int j = 0; j < numOfDobri; j++) {

                                    Classifier.Recognition res = results.get(j);

                                    RectF cords = res.getLocation();

                                    CordsInt.add(popraviCords(cords, oriSizeX, oriSizeY, INPUT_SIZE));

                                }

                                Bitmap imageBlur = BlurFaces.blurFaces(bitmap, CordsInt, context);
                                encoder.encodeImage(imageBlur);

                            }
                        }
                        catch (Exception e){
                            System.out.println("Exception= " + e);
                        }

                    }
                    try {
                        encoder.finish();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                };
            };
            Thread mythread = new Thread(runnable);

            mythread.start();
            mythread.join();

        } catch (final Exception e) {
            System.out.println("Exception= " + e);
        }
        NIOUtils.closeQuietly(out);

        Uri uri = Uri.parse(Environment.getExternalStorageDirectory()+ VIDEO_DIRECTORY + "/" + fileName);

        Intent intent2 = new Intent(Processing.this, VideoPlay.class);
        intent2.putExtra("videoURI1", uri);

        intent2.putExtra("videoName", fileName);
        startActivity(intent2);
    }

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

        public void initTensorFlowAndLoadModel() {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        classifier = TFLiteObjectDetectionAPIModel.create(
                                getAssets(),
                                MODEL_FILE,
                                LABEL_FILE,
                                INPUT_SIZE,
                                TF_OD_API_IS_QUANTIZED,
                                MIN_CONFIDENCE);
                    } catch (final Exception e) {
                        throw new RuntimeException("Error initializing TensorFlow!", e);
                    }
                }
            });
        }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public String  getPath(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    public List<Integer> popraviCords (RectF cords, int startSizeX, int startSizeY, int endSize){
        List<Integer> popC = new ArrayList<>();
        popC.add(Math.max(Math.round(cords.left*startSizeX/endSize),0));
        popC.add(Math.max(Math.round(cords.top*startSizeY/endSize),0));
        popC.add(Math.min(Math.round(cords.right*startSizeX/endSize), startSizeX));
        popC.add(Math.min(Math.round(cords.bottom*startSizeY/endSize), startSizeY));
        return popC;

    }
}