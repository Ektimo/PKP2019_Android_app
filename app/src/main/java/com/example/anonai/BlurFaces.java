package com.example.anonai;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import java.util.List;

import static java.security.AccessController.getContext;

public class BlurFaces {
    public static Bitmap blurFaces(Bitmap bitmap, List<List<Integer>> cords, Context context ){
        Bitmap bmOverlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bitmap, new Matrix(), null);
        for (int i = 0; i < cords.size(); i++) {
            // odrežemo vse kar ni obraz
            List<Integer> cord = cords.get(i);
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, cord.get(0), cord.get(1), cord.get(2) -cord.get(0), cord.get(3) - cord.get(1));

            //bluranje slike

            Bitmap blurBitmap = blurBitmap(context, croppedBitmap);

            // vrne frame z zablurabim obrazom
            canvas.drawBitmap(blurBitmap, null, new Rect(cord.get(0), cord.get(1), cord.get(2), cord.get(3)), null);
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
        blurScript.setRadius(25.f);


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
/*    private static Bitmap overlay(Bitmap bmp1, Bitmap bmp2, int x1, int  y1, int x2, int  y2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, null, new Rect(x1, y1, x2, y2), null);
        return bmOverlay;
    }*/
}
