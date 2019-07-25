package com.example.anonai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicResize;
import android.renderscript.Type;


import java.util.List;

public class BlurFaces {
    public static Bitmap blurFaces(Bitmap bitmap, List<List<Integer>> cords, Context context ){
        Bitmap bmOverlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bitmap, new Matrix(), null);
        for (int i = 0; i < cords.size(); i++) {
            // odrežemo vse kar ni obraz/prepoznan objekt
            List<Integer> cord = cords.get(i);
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, cord.get(0), cord.get(1), cord.get(2) -cord.get(0), cord.get(3) - cord.get(1));

            //blur image

            Bitmap blurBitmap = blurBitmap(context, croppedBitmap);

            Bitmap superBlurBitmap = blurResize(context, blurBitmap);


            canvas.drawBitmap(superBlurBitmap, null, new Rect(cord.get(0), cord.get(1), cord.get(2), cord.get(3)), null);

            //canvas.drawBitmap(blurBitmap, null, new Rect(cord.get(0), cord.get(1), cord.get(2), cord.get(3)), null);
        }
        return bmOverlay;


    };



    public static Bitmap blurBitmap(Context context, Bitmap bitmap){

//Let’s create an empty bitmap with the same size of the bitmap we want to blur
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(),   bitmap.getHeight(), Bitmap.Config.ARGB_8888);


//Instantiate a new Renderscript
        RenderScript rs = RenderScript.create(context);


//Create an Intrinsic Blur Script using the Renderscript
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));


//Create the in/out Allocations with the Renderscript and the in/out bitmaps
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);


//Set the radius of the blur
        blurScript.setRadius(50.f);


//Perform the Renderscript
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);


//Copy the final bitmap created by the out Allocation to the outBitmap
        allOut.copyTo(outBitmap);


//recycle the original bitmap
        bitmap.recycle();


//After finishing everything, we destroy the Renderscript.
        rs.destroy();

        return outBitmap;

    }

    // ne dela še najboljše
    private static final float BITMAP_SCALE = 0.7f;
    private static final float RESIZE_SCALE = 1.f/5.f;
    private static RenderScript rs;

    public static Bitmap blurResize(Context context, Bitmap image) {
        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        if (rs == null) {
            // Creating a RS context is expensive, better reuse it.
            rs = RenderScript.create(context);
        }
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);

        Type t = Type.createXY(rs, tmpIn.getElement(),  (int) (width*RESIZE_SCALE),  (int) (height*RESIZE_SCALE));
        Allocation tmpScratch = Allocation.createTyped(rs, t);

        ScriptIntrinsicResize theIntrinsic = ScriptIntrinsicResize.create(rs);
        // Resize the original img down.
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach_bicubic(tmpScratch);
        // Resize smaller img up.
        theIntrinsic.setInput(tmpScratch);
        theIntrinsic.forEach_bicubic(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }

}
