package com.msu.initialization;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by sagesmith on 12/16/16.
 */

public class PictureResult {
    public String side;
    private PictureResult(String side) {
        this.side = side;
    }

    public static PictureResult process(byte[] data) {
        if (data == null)
            return null;

        Log.d("Bitmap", "Started decoding");
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        Log.d("Bitmap Size", bitmap.getWidth() + " " + bitmap.getHeight());

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true);
        bitmap.recycle();
        bitmap = scaledBitmap;
        Log.d("Bitmap", "Done decoding");
        try {
            Integer leftEdge = findLeftEdge(bitmap);
            if (leftEdge == null) {
                return new PictureResult(null);
            }
            if (leftEdge > bitmap.getWidth() / 2) {
                return new PictureResult("right");
            }

            Integer rightEdge = findRightEdge(bitmap);
            int center = (leftEdge + rightEdge) / 2;
            if (center > bitmap.getWidth() / 2) {
                return new PictureResult("right");
            }

            return new PictureResult("left");
        } finally {
            Log.d("Bitmap", "Done searching");
            bitmap.recycle();
        }
    }

    public static Integer findLeftEdge(Bitmap bitmap) {
        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                if (threshhold(bitmap.getPixel(x, y))) {
                    return x;
                }
            }
        }
        return null;
    }

    public static Integer findRightEdge(Bitmap bitmap) {
        for (int x = bitmap.getWidth() -1; x >= 0; x--) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                if (threshhold(bitmap.getPixel(x, y))) {
                    return x;
                }
            }
        }
        return null;
    }

    public static boolean threshhold(int color) {
        return Color.green(color) > 0xE0 && Color.red(color) < 0x20 && Color.blue(color) < 0x20;
    }
}
