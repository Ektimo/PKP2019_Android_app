package com.example.anonai;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;


import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import android.util.Log;

import android.widget.Button;
import android.widget.VideoView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    private Button btn2;
    private Button btn4;
    private Button btn3;
    private VideoView videoView;
    private static final String VIDEO_DIRECTORY = "/anonai";
    private int GALLERY = 1, CAMERA = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btn2 = (Button) findViewById(R.id.button2);
        btn3 = (Button) findViewById(R.id.button3);
        btn4 = (Button) findViewById(R.id.button4);
        videoView = (VideoView) findViewById(R.id.vv);

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeVideoFromCamera();
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseVideoFromGallery();
            }
        });
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {loadModel();}
                catch (Exception e){
                    System.out.println(e);
                }
            }
        });
    }

    public void chooseVideoFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takeVideoFromCamera() {
        System.out.println("odpiram kamero");
        Intent intent = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
        startActivityForResult(intent, CAMERA);

    }

    private void loadModel() throws IOException {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);
        File file =  new File(directory,"tensorflow_inception_graph.pb");
        String data = "tensorflow_inception_graph";
        FileOutputStream fos = new FileOutputStream("tensorflow_inception_graph.pb", true); // save
        fos.write(data.getBytes());
        fos.close();



    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("result",""+resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            Log.d("what","cancel");
            return;
        }
        if (requestCode == GALLERY) {
            Log.d("what","gale");
            if (data != null) {
                Uri contentURI = data.getData();
                System.out.println("-> Processing");

//                Intent intent1 = new Intent(MainActivity.this, Processing.class);
//
//                startActivity(intent1 );
                Intent intent = new Intent(MainActivity.this, Processing.class);
                intent.putExtra("videoURI", contentURI);
                startActivity(intent);

            }

        } else if (requestCode == CAMERA) {
            System.out.println("je kamera");
            Uri contentURI = data.getData();
            String recordedVideoPath = getPath(contentURI);
            Log.d("frrr",recordedVideoPath);
            saveVideoToInternalStorage(recordedVideoPath);
            videoView.setVideoURI(contentURI);
            videoView.requestFocus();
            videoView.start();

        }

    }

    private void saveVideoToInternalStorage (String filePath) {

        File newfile;

        try {

            File currentFile = new File(filePath);
            File wallpaperDirectory = new File(Environment.getExternalStorageDirectory() + VIDEO_DIRECTORY);
            newfile = new File(wallpaperDirectory, Calendar.getInstance().getTimeInMillis() + ".mp4");

            if (!wallpaperDirectory.exists()) {
                wallpaperDirectory.mkdirs();
            }

            if(currentFile.exists()){

                InputStream in = new FileInputStream(currentFile);
                OutputStream out = new FileOutputStream(newfile);

                // Copy the bits from instream to outstream
                byte[] buf = new byte[1024];
                int len;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                Log.v("vii", "Video file saved successfully.");
            }else{
                Log.v("vii", "Video saving failed. Source file missing.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}