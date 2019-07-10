package com.example.anonai;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

public class LiveCamera2 extends AppCompatActivity implements
        TextureView.SurfaceTextureListener {

    private CameraDevice mCamera;
    private String mCameraID = "1";

    private ImageView imageView;

    private TextureView mPreviewView;
    private Size mPreviewSize;
    private CaptureRequest.Builder mPreviewBuilder;
    private ImageReader mImageReader;

    private Handler mHandler;
    private HandlerThread mThreadHandler;

    // size of images captured in ImageReader Callback
    private int mImageWidth = 1920; //1920
    private int mImageHeight = 1080; //1080

    private int[] rgbBytes = null;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private Bitmap rgbFrameBitmap;

    public Executor executor = Executors.newSingleThreadExecutor();
    public Classifier classifier;


    static final int kMaxChannelValue = 262143;


    //surface
    private Surface surface;
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_camera2);
        imageView = (ImageView) findViewById(R.id.imageView5);

        initView();
        initLooper();

        initTensorFlowAndLoadModel();

    }

    // subroutine to run camera
    private void initLooper() {
        mThreadHandler = new HandlerThread("CAMERA2");
        mThreadHandler.start();
        mHandler = new Handler(mThreadHandler.getLooper());
    }

    /*
     *  step 2: using TextureView to display PREVIEW
     */
    private void initView() {
        mPreviewView = (TextureView) findViewById(R.id.textureview);
        mPreviewView.setSurfaceTextureListener(this);
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            //new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CAMERA_PERMISSION: {
                //If request is cancelled, the result arrays are empty.
                if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //permission was granted, do the contacts-related task you want to do.
                } else {
                    //permission was denied, disable the functionality that depends on thhis permission.
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
            }
            //other 'case' lines to check for other permissions this app might request
        }
    }

    /*
     *  step 3: to set features of camera and open camera
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                          int height) {
        try {
            // to get the manager of all cameras
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            // to get features of the selected camera
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(mCameraID);
            // to get stream configuration from features
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // to get the size that the camera supports
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            // open camera
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                requestCameraPermission();
                return;
            }
            cameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, mHandler);

















            // do sem ne pridemo nikoli
            // ne vem katero kamero se uporablja, morda se da spremenit
            // mi je uspelo prit do sem, cameraManager pravi da nima razpoložljive kamere


            CameraCharacteristics b = cameraManager.getCameraCharacteristics("1");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                            int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    // note that the following method will be called every time a frame's ready
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            try {
                mCamera = camera;
                startPreview(mCamera);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };

    /*
     *  step 4: to start PREVIEW
     */
    private void startPreview(CameraDevice camera) throws CameraAccessException {
        SurfaceTexture texture = mPreviewView.getSurfaceTexture();

        // to set PREVIEW size
        texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        surface = new Surface(texture);
        try {
            // to set request for PREVIEW
            mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // to set the format of captured images and the maximum number of images that can be accessed in mImageReader
        mImageReader = ImageReader.newInstance(mImageWidth, mImageHeight, ImageFormat.YUV_420_888, 2);

        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,mHandler);

        // the first added target surface is for camera PREVIEW display
        // the second added target mImageReader.getSurface() is for ImageReader Callback where we can access EACH frame
        //mPreviewBuilder.addTarget(surface);
        mPreviewBuilder.addTarget(mImageReader.getSurface());

        //output Surface
        List<Surface> outputSurfaces = new ArrayList<>();
        outputSurfaces.add(mImageReader.getSurface());

        /*camera.createCaptureSession(
                Arrays.asList(surface, mImageReader.getSurface()),
                mSessionStateCallback, mHandler);
                */
        camera.createCaptureSession(outputSurfaces, mSessionStateCallback, mHandler);
    }


    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                updatePreview(session);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    private void updatePreview(CameraCaptureSession session)
            throws CameraAccessException {
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        session.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
    }


    /*
     *  step 5: to implement listener and access each frame
     */
    /*private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

     *//*
     *  The following method will be called every time an image is ready
     *  be sure to use method acquireNextImage() and then close(), otherwise, the display may STOP
     *//*
        @Override
        public void onImageAvailable(ImageReader reader) {
            // get the newest frame
            Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            Image.Plane[] planes = image.getPlanes();

            int width = image.getWidth();
            int height = image.getHeight();

            //byte[] bytes = yuvImageToByteArray(image);

            //int[] pixels = convertYUV420_NV21toRGB8888(bytes, width, height);

            //Bitmap bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);


//          poskus, ne dela ker "buffer not large enough for pixels"
            *//*int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ALPHA_8);
            ByteBuffer buffer = planes[0].getBuffer();

            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);*//*



            // HERE to call jni methods
            //grayscale output
//            JNIUtils.GrayscaleDisplay(image.getWidth(), image.getHeight(), image.getPlanes()[0].getRowStride(), image.getPlanes()[0].getBuffer(), surface);

            //RGBA output
//            Image.Plane Y_plane = image.getPlanes()[0];
//            int Y_rowStride = Y_plane.getRowStride();
//            Image.Plane U_plane = image.getPlanes()[1];
//            int UV_rowStride = U_plane.getRowStride();  //in particular, uPlane.getRowStride() == vPlane.getRowStride()
//            Image.Plane V_plane = image.getPlanes()[2];
//            JNIUtils.RGBADisplay(image.getWidth(), image.getHeight(), Y_rowStride, Y_plane.getBuffer(), UV_rowStride, U_plane.getBuffer(), V_plane.getBuffer(), surface);

//            JNIUtils.RGBADisplay2(image.getWidth(), image.getHeight(), Y_rowStride, Y_plane.getBuffer(), U_plane.getBuffer(), V_plane.getBuffer(), surface);

//            Log.d(TAG, "Y plane pixel stride: " + Y_plane.getPixelStride());
//            Log.d(TAG, "U plane pixel stride: " + U_plane.getPixelStride());
//            Log.d(TAG, "V plane pixel stride: " + V_plane.getPixelStride());

//            Log.d(TAG, "Y plane length: " + Y_plane.getBuffer().remaining());
//            Log.d(TAG, "U plane length: " + U_plane.getBuffer().remaining());
//            Log.d(TAG, "V plane length: " + V_plane.getBuffer().remaining());

//            Log.d(TAG, "Y plane rowStride: " + Y_rowStride);
//            Log.d(TAG, "U plane rowStride: " + U_rowStride);
//            Log.d(TAG, "V plane rowStride: " + V_rowStride);


            image.close();
        }
    };*/

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        /*
         *  The following method will be called every time an image is ready
         *  be sure to use method acquireNextImage() and then close(), otherwise, the display may STOP
         */
        @Override
        public void onImageAvailable(ImageReader reader) {
            if (rgbBytes == null) {
                rgbBytes = new int[mImageWidth * mImageHeight];
            }
            try {
                final Image image = reader.acquireLatestImage();

                if (image == null) {
                    return;
                }

                if (isProcessingFrame) {
                    image.close();
                    return;
                }
                isProcessingFrame = true;
                final Image.Plane[] planes = image.getPlanes();
                fillBytes(planes, yuvBytes);
                yRowStride = planes[0].getRowStride();
                final int uvRowStride = planes[1].getRowStride();
                final int uvPixelStride = planes[1].getPixelStride();

                imageConverter =
                        new Runnable() {
                            @Override
                            public void run() {
                                convertYUV420ToARGB8888(
                                        yuvBytes[0],
                                        yuvBytes[1],
                                        yuvBytes[2],
                                        mImageWidth,
                                        mImageHeight,
                                        yRowStride,
                                        uvRowStride,
                                        uvPixelStride,
                                        rgbBytes);
                            }
                        };

                postInferenceCallback =
                        new Runnable() {
                            @Override
                            public void run() {
                                image.close();
                                isProcessingFrame = false;
                            }
                        };

                processImage();
            } catch (final Exception e) {
                return;
            }
        };
    };


    protected void onPause() {
        if (null != mCamera) {
            mCamera.close();
            mCamera = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
        super.onPause();
    }

    /**
     * Converts YUV420 NV21 to RGB8888
     *
     * @param data byte array on YUV420 NV21 format.
     * @param width pixels width
     * @param height pixels height
     * @return a RGB8888 pixels int array. Where each int is a pixels ARGB.
     */
    public static int[] convertYUV420_NV21toRGB8888(byte [] data, int width, int height) {
        int size = width*height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i percorre os Y and the final pixels
        // k percorre os pixles U e V
        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;

            u = data[offset+k  ]&0xff;
            v = data[offset+k+1]&0xff;
            u = u-128;
            v = v-128;

            pixels[i  ] = convertYUVtoRGB(y1, u, v);
            pixels[i+1] = convertYUVtoRGB(y2, u, v);
            pixels[width+i  ] = convertYUVtoRGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoRGB(y4, u, v);

            if (i!=0 && (i+2)%width==0)
                i+=width;
        }

        return pixels;
    }

    private static int convertYUVtoRGB(int y, int u, int v) {
        int r,g,b;

        r = y + (int)(1.402f*v);
        g = y - (int)(0.344f*u +0.714f*v);
        b = y + (int)(1.772f*u);
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (b<<16) | (g<<8) | r;
    }

    public static byte[] yuvImageToByteArray(Image image) {

        assert(image.getFormat() == ImageFormat.YUV_420_888);

        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane[] planes = image.getPlanes();
        byte[] result = new byte[width * height * 3 / 2];

        int stride = planes[0].getRowStride();
        assert (1 == planes[0].getPixelStride());
        if (stride == width) {
            planes[0].getBuffer().get(result, 0, width*height);
        }
        else {
            for (int row = 0; row < height; row++) {
                planes[0].getBuffer().position(row*stride);
                planes[0].getBuffer().get(result, row*width, width);
            }
        }

        stride = planes[1].getRowStride();
        assert (stride == planes[2].getRowStride());
        int pixelStride = planes[1].getPixelStride();
        assert (pixelStride == planes[2].getPixelStride());
        byte[] rowBytesCb = new byte[stride];
        byte[] rowBytesCr = new byte[stride];

        for (int row = 0; row < height/2; row++) {
            int rowOffset = width*height + width/2 * row;
            planes[1].getBuffer().position(row*stride);
            planes[1].getBuffer().get(rowBytesCb);
            planes[2].getBuffer().position(row*stride);
            planes[2].getBuffer().get(rowBytesCr);

            for (int col = 0; col < width/2; col++) {
                result[rowOffset + col*2] = rowBytesCr[col*pixelStride];
                result[rowOffset + col*2 + 1] = rowBytesCb[col*pixelStride];
            }
        }
        return result;
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public static void convertYUV420ToARGB8888(
            byte[] yData,
            byte[] uData,
            byte[] vData,
            int width,
            int height,
            int yRowStride,
            int uvRowStride,
            int uvPixelStride,
            int[] out) {
        int yp = 0;
        for (int j = 0; j < height; j++) {
            int pY = yRowStride * j;
            int pUV = uvRowStride * (j >> 1);

            for (int i = 0; i < width; i++) {
                // pri j=718, i=1280 se sesuje. offset=460800 (za 1 prevelik)
                int uv_offset = pUV + (i >> 1) * uvPixelStride;

                if (uv_offset < 460800) {
                    out[yp] = YUV2RGB(0xff & yData[pY + i], 0xff & uData[uv_offset], 0xff & vData[uv_offset]);
                }
                yp++;
                if (i==1279) {
                    //System.out.println("i");
                }
            }
            if (j==717) {
                //System.out.println("hopla");
            }
        }
        //System.out.println("ka bo kej");
    }

    private static int YUV2RGB(int y, int u, int v) {
        // Adjust and check YUV values
        y = (y - 16) < 0 ? 0 : (y - 16);
        u -= 128;
        v -= 128;

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
        g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
        b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    }

    protected void processImage() {

        int[] a = getRgbBytes();
        rgbFrameBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        rgbFrameBitmap.setPixels(a, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight);
        //System.out.println(detect(rgbFrameBitmap));
        runOnUiThread(new Runnable(){
            public void run() {
                imageView.setImageBitmap(rgbFrameBitmap);
                imageView.setVisibility(View.VISIBLE);

                // to ni v redu
                Drawable d = new BitmapDrawable(getResources(), rgbFrameBitmap);
                mPreviewView.setBackgroundDrawable(d);
            }
        });
        postInferenceCallback.run();
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected List<Classifier.Recognition> detect(Bitmap bitmap) {
        Context context = getApplicationContext();

        int oriSizeX = bitmap.getWidth();
        int oriSizeY = bitmap.getHeight();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, Processing.INPUT_SIZE, Processing.INPUT_SIZE, false);

        try {
            List<Classifier.Recognition> results = classifier.recognizeImage(scaledBitmap);
            System.out.println(results.toString());
            int numOfRes = results.size();
            List<Classifier.Recognition> dobriRes = new ArrayList<Classifier.Recognition>();

            for (int k = 0; k < numOfRes; k++) {
                if (results.get(k).getConfidence() > Processing.MIN_CONFIDENCE && results.get(k).getConfidence() < 1.1f) {
                    dobriRes.add(results.get(k));
                } else {
                    break;
                }
            }

            List<List<Integer>> CordsInt = new ArrayList<List<Integer>>();

            int numOfDobri = dobriRes.size();

            for (int j = 0; j < numOfDobri; j++) {

                Classifier.Recognition res = results.get(j);

                RectF cords = res.getLocation();

                CordsInt.add(Processing.popraviCords(cords, oriSizeX, oriSizeY, Processing.INPUT_SIZE));

            }

            Bitmap imageBlur = BlurFaces.blurFaces(bitmap, CordsInt, context);
            return dobriRes;

        } catch (Exception e) {
            System.out.println("Exception= " + e);
            return null;
        }
    }

    public void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            Processing.MODEL_FILE,
                            Processing.LABEL_FILE,
                            Processing.INPUT_SIZE,
                            Processing.TF_OD_API_IS_QUANTIZED,
                            Processing.MIN_CONFIDENCE);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

}