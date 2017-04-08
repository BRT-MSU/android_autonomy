package nasa_rmc.autonomy.logic.logicState;

/**
 * Created by Joe on 4/8/2017.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by sagesmith on 12/16/16.
 */

public class PictureResult {
    public String side;
    public int left, right;
    public static final float PERCENTAGE_HEIGHT_THRESHOLD = 0.15f;
    public static final float PERCENTAGE_WIDTH_THRESHOLD = 0.15f;

    private PictureResult(String side, int left, int right) {
        this.side = side;
        this.left = left;
        this.right = right;
    }

    public static PictureResult process(byte[] data, int id) {
        if (data == null)
            return null;

        Log.d("Bitmap", "Started decoding");
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        Log.d("Bitmap Size", bitmap.getWidth() + " " + bitmap.getHeight());

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true);

        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        if (id == 1) {
            matrix.postScale(-1, 1);
        }

        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, 100, 100, matrix, true);

        bitmap.recycle();
        scaledBitmap.recycle();
        bitmap = rotatedBitmap;
        Log.d("Bitmap", "Done decoding");
        try {
            Integer leftEdge = findLeftEdge(bitmap);
            Integer rightEdge = findRightEdge(bitmap);
            if (leftEdge == null || rightEdge == null ||
                (float)(leftEdge-rightEdge)/(float)bitmap.getWidth() < PERCENTAGE_WIDTH_THRESHOLD) {
                return new PictureResult(null, -1, -1);
            }
            if (leftEdge > bitmap.getWidth() / 2) {
                return new PictureResult("A", leftEdge, -1);
            }

            int center = (leftEdge + rightEdge) / 2;
            if (center > bitmap.getWidth() / 2) {
                return new PictureResult("A", leftEdge, rightEdge);
            }

            return new PictureResult("B", leftEdge, rightEdge);
        } finally {
            Log.d("Bitmap", "Done searching");
            bitmap.recycle();
        }
    }

    public static Integer findLeftEdge(Bitmap bitmap) {
        int counter = 0;
        for (int x = 0; x < bitmap.getWidth(); x++) {
            counter = 0;
            for (int y = 0; y < bitmap.getHeight(); y++) {
                if (threshhold(bitmap.getPixel(x, y))) {
                    counter++;
                }
            }
            if((float)counter/(float)bitmap.getWidth() > PERCENTAGE_HEIGHT_THRESHOLD){
                return x;
            }
        }
        return null;
    }

    public static Integer findRightEdge(Bitmap bitmap) {
        int counter = 0;
        for (int x = bitmap.getWidth() -1; x >= 0; x--) {
            counter = 0;
            for (int y = 0; y < bitmap.getHeight(); y++) {
                if (threshhold(bitmap.getPixel(x, y))) {
                    counter++;
                }
            }
            if((float)counter/(float)bitmap.getWidth() > PERCENTAGE_HEIGHT_THRESHOLD){
                return x;
            }
        }
        return null;
    }

    public static boolean threshhold(int color) {
        int red = Color.red(color);
        int blue = Color.blue(color);
        int green = Color.green(color);
        return green > 50 && green - red > 20 && green - blue > 20;
    }
}
