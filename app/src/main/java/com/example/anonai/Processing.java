package com.example.anonai;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.wonderkiln.camerakit.CameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;

import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Processing extends AppCompatActivity {
    public static final int INPUT_SIZE = 224;
    public static final int IMAGE_MEAN = 117;
    public static final float IMAGE_STD = 1;
    public static final String INPUT_NAME = "input";
    public static final String OUTPUT_NAME = "output";
    private static final String VIDEO_DIRECTORY = "/anonai";

    //private static final String MODEL_FILE = Environment.getExternalStorageDirectory() + "/assets/tensorflow_inception_graph.pb";
    //private static final String LABEL_FILE = Environment.getExternalStorageDirectory() + "/assets/imagenet_comp_graph_label_strings.txt";

    public static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    public static final String LABEL_FILE = "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    //public static final String MODEL_FILE = "file:///android_asset/detect.tflite";
    //public static final String LABEL_FILE = "file:///android_asset/labelmap.txt";

    public Classifier classifier;
    public ImageClassifier imageClassifier;
    public Executor executor = Executors.newSingleThreadExecutor();
    private CameraView cameraView;
    private TextView textViewResult;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textViewResult = (TextView) findViewById(R.id.textViewResult);
        //textViewResult.setMovementMethod(new ScrollingMovementMethod());


        Intent intent = getIntent();
        Uri contentURI = intent.getParcelableExtra("videoURI");

        // .pb model
        /*initTensorFlowAndLoadModel();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Ustvaril classifier");*/

        // Koda za zajem slik z videa

        final ArrayList<Bitmap> frameList = new ArrayList<>();

        // MediaMetadataRetriever class is used to retrieve meta data from methods. *//*
        final MediaMetadataRetriever retriever = new MediaMetadataRetriever();


        try {
            //path of the video of which you want frames
            retriever.setDataSource(this, contentURI);
            System.out.println("neki je");
        } catch (Exception e) {
            System.out.println("Exception= " + e);
        }
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        String numberOfFrames = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);
        final int NOF = Integer.parseInt(numberOfFrames);
        int duration_millisec = Integer.parseInt(duration); //duration in millisec
        int frames_per_second = 10;  //no. of frames want to retrieve per second
        final int numeroFrameCaptured = frames_per_second * (duration_millisec / 1000);



        // .pb model
        /*for (int i = 0; i < numeroFrameCaptured; i++) {
            frameList.add(retriever.getFrameAtIndex(i * NOF / numeroFrameCaptured));
            System.out.println("dodal " + i);

            Bitmap bitmap = frameList.get(i);
            Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
            try {List<Classifier.Recognition> results = classifier.recognizeImage(bitmap1);
                System.out.println(i + " " + results.toString());
                textViewResult.setText(results.toString());
            }
            catch (Exception e){
                System.out.println("problem" + e);
            }
        }*/

        final Context context = getApplicationContext();

        FileChannelWrapper out = null;
        String fileName = getFileName(contentURI);


        try {
            File root = new File(Environment.getExternalStorageDirectory() + VIDEO_DIRECTORY);
            File dir = new File(root.getAbsolutePath());
            System.out.println(dir);


            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(dir, fileName);
            String path = file.getAbsolutePath();
            out = NIOUtils.writableFileChannel(path);
            // for Android use: AndroidSequenceEncoder
            final AndroidSequenceEncoder encoder = new AndroidSequenceEncoder(out, Rational.R(numeroFrameCaptured, (duration_millisec / 1000)));

            // .tflite model
            try {
                imageClassifier = new ImageClassifier(this);
            } catch (final IOException e) {
                System.out.println(e.getStackTrace());
            }
            Runnable runnable = new Runnable() {

                public void run() {
                    for (int i = 0; i < numeroFrameCaptured; i++) {
                        frameList.add(retriever.getFrameAtIndex(i * NOF / numeroFrameCaptured));

                        Bitmap bitmap = frameList.get(i);
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                        try {
                            String result = imageClassifier.classifyFrame(scaledBitmap);
                            System.out.println(i + " " + result);
                        } catch (Exception e) {
                            System.out.println("problem" + e);
                        }

                        Bitmap imageBlur = BlurFaces.blurFaces(bitmap, 50, 50, 500, 500, context);
                        try {
                            encoder.encodeImage(imageBlur);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                };
            };
            Thread mythread = new Thread(runnable);

            mythread.start();
            mythread.join();

            imageClassifier.close();
                    try {
                        encoder.finish();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } catch(
                final Exception e)

                {
                    System.out.println(e.getStackTrace());
                }
        NIOUtils.closeQuietly(out);


        // trenutno printa seznam zaznanih objektov in verjetnosti, včasih se sesuje


        /*for (int i = 0; i < numeroFrameCaptured; i++) {
            Bitmap bitmap = frameList.get(i);
            Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
            *//*try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*//*

            *//*try {
                ImageClassifier classifier = new ImageClassifier(this);
                String results = classifier.classifyFrame(bitmap1);
                System.out.println(results);
            } catch (Exception e) {
                e.printStackTrace();
            }*//*

            try {List<Classifier.Recognition> results = classifier.recognizeImage(bitmap1);
                System.out.println(i + " " + results.toString());
                textViewResult.setText(results.toString());
             }
            catch (Exception e){
                System.out.println("problem" + e);
            }
        }*/

        //rekonstrukcija videa
        /*try {

            File root= new File(Environment.getExternalStorageDirectory()+VIDEO_DIRECTORY);
            File dir = new File(root.getAbsolutePath());
            System.out.println(dir);


            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(dir, fileName);
            String path = file.getAbsolutePath();
            out = NIOUtils.writableFileChannel(path);
            // for Android use: AndroidSequenceEncoder
            AndroidSequenceEncoder encoder = new AndroidSequenceEncoder(out, Rational.R(numeroFrameCaptured, (duration_millisec / 1000))); // video je nekaj sekund krajši, 8s -> 7s. 5s-> 3s?????
            for (int i = 0; i < numeroFrameCaptured; i++) {
                // Generate the image, for Android use Bitmap
                Bitmap image = frameList.get(i);


                Bitmap imageBlur = BlurFaces.blurFaces(image, 50, 50, 500, 500, context);
                // Encode the image
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                };
                encoder.encodeImage(imageBlur);
            }
            // Finalize the encoding, i.e. clear the buffers, write the header, etc.
            encoder.finish();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            NIOUtils.closeQuietly(out);
        }*/

        Uri uri = Uri.parse(Environment.getExternalStorageDirectory()+ VIDEO_DIRECTORY + "/" + fileName);

        Intent intent2 = new Intent(Processing.this, VideoPlay.class);
        intent2.putExtra("videoURI1", uri);

        intent2.putExtra("videoName", fileName);
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

        public void initTensorFlowAndLoadModel() {
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


}
