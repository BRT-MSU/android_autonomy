package nasa_rmc.autonomy.logic.logicState;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.Manifest;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.view.View.GONE;

import nasa_rmc.autonomy.logic.LogicContext;

/**
 * Created by atomlinson on 3/31/17.
 */

public class InitializeState implements LogicState {
    private LogicContext logicContext;

    private String status;
    public String initPos;
    public String getStatus() { return status; }

    public InitializeState(LogicContext logicContext) {
        this.logicContext = logicContext;
    }

    @Override
    public void run() throws InterruptedException {
        status = "Initializing.";
        TimeUnit.SECONDS.sleep(2);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 0);
        } else {
            //getCameras();
        }

        takePicture(0);
        //writeServo(0);

        // Initialization should return variables like these (x translation, y translation, and angle)
        double tangoXTranslationAdjustment = 2.84;
        double tangoYTranslationAdjustment = 0.75;
        double tangoAngleAdjustment = 0.0;

        //TODO: edit above variables based on initPos and emperical data

        logicContext.getData().setTangoXTranslationAdjustment(tangoXTranslationAdjustment);
        logicContext.getData().setTangoYTranslationAdjustment(tangoYTranslationAdjustment);
        logicContext.getData().setTangoAngleAdjustment(tangoAngleAdjustment);

        logicContext.setLogicState(logicContext.getDriveState());
        logicContext.getLogicState().run();
    }

       /*public void getCameras() {
        final CameraManager cameraManager = (CameraManager)getSystemService(CAMERA_SERVICE);
        try {
            String[] cameras = cameraManager.getCameraIdList();
/*            for (int i = 0; i < cameras.length; i++) {
                Toast.makeText(this, "Camera " + i + ": " + cameras[i], Toast.LENGTH_SHORT).show();
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameras[i]);
                Integer orientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Toast.makeText(this, "Orientation: "+orientation, Toast.LENGTH_SHORT).show();
                Integer direction = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                String directionStr = (direction == LENS_FACING_FRONT ? "FRONT" : "BACK");
                Toast.makeText(this, "Direction: " + directionStr, Toast.LENGTH_SHORT).show();
            }
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Couldn't list cameras", Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Unexpected Security!", Toast.LENGTH_LONG).show();
        }
    }*/

    private SizeF getFieldOfView(CameraCharacteristics cameraCharacteristics) {

        float focalLength = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0];
        //Log.d("Camera", "Focal Length: "+focalLength);

        SizeF physicalArraySize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        //Log.d("Camera", "Physical Array Size: " + physicalArraySize.toString());

        return new SizeF(
                (float)(2 * Math.toDegrees(Math.atan( physicalArraySize.getWidth() / (2 * focalLength)))),
                (float)(2 * Math.toDegrees(Math.atan( physicalArraySize.getHeight() / (2 * focalLength))))
        );
    }

    private void takePicture(final int id) {
        final Camera cam = Camera.open(id);
        cam.setDisplayOrientation(90);
        try {
            cam.setPreviewDisplay(holder);
        } catch (IOException e){}
        cam.enableShutterSound(false);
        cam.startPreview();
        cam.takePicture(null, null,
                new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        cam.stopPreview();
                        cam.release();

                        getPictureResult(id, PictureResult.process(data, id));
                    }
                });
    }

    private int curPosition = 0;
    private void getPictureResult(int id, PictureResult result) {
        //Toast.makeText(this, "Got picture: "+id+ " "+result.side+" "+result.left+" "+result.right, Toast.LENGTH_SHORT).show();
        if (result.side == null) {
            if (curPosition == 90 && id == 1) {
                initPos = "No sign";
                return;
            }
            if (id == 1) {
                curPosition += 90;
                //writeServo(curPosition);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        takePicture(0);
                    }
                }, 1000);
            } else {
                takePicture(id + 1);
            }
        } else {
            String direction = null;
            if (curPosition == 0 && id == 0) {
                direction = "north";
            } else if (curPosition == 0 && id == 1) {
                direction = "south";
            } else if (curPosition == 90 && id == 0) {
                direction = "west";
            } else if (curPosition == 90 && id == 1) {
                direction = "east";
            }
            initPos = result.side+ ": "+direction;
        }
    }

    private void ezWait(Object o) {
        try {
            synchronized (o) {
                o.wait();
            }
        } catch (InterruptedException e) { }
    }
}
