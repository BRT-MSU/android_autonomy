package com.msu.initialization;

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

public class MainActivity extends AppCompatActivity {
    private UsbSerialDevice serialConnection;
    private SurfaceHolder holder;
    private TextView resultView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        holder = ((SurfaceView)findViewById(R.id.surfaceView)).getHolder();
        final Button pictureButton = (Button)findViewById(R.id.takePicture);
        resultView = (TextView) findViewById(R.id.resultView);
        //SurfaceView view = new SurfaceView(this);
        //holder = view.getHolder();
        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pictureButton.setVisibility(GONE);
                takePicture(0);
            }
        });
        writePosition(0);
        /*holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        holder.setFormat(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);*/
        /*final TextView servoPosition = (TextView)findViewById(R.id.servoPosition);
        final SeekBar positionBar = (SeekBar)findViewById(R.id.position);
        positionBar.setMax(180);

        positionBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                servoPosition.setText(""+progress);
                writePosition(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //writePosition(seekBar.getProgress());
            }
        });

*/

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 0);
        } else {
            //getCameras();
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Got Camera Permission", Toast.LENGTH_SHORT).show();
            //getCameras();
        } else if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Couldn't get Camera permission", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "What?", Toast.LENGTH_SHORT).show();
        }
    }

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

    private void writePosition(int position) {
        if (serialConnection == null) {
            setupSerialConnection();
        }
        if (serialConnection != null) {
            serialConnection.write((position + ".").getBytes());
        }
    }

    private void setupSerialConnection() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDevice device = getUsbDevice(manager);

        if (device == null)
            return;

        UsbDeviceConnection connection = manager.openDevice(device);
        serialConnection = UsbSerialDevice.createUsbSerialDevice(device, connection);

        if (serialConnection == null) {
            Toast.makeText(MainActivity.this, "Could not make serial connection", Toast.LENGTH_LONG).show();
            return;
        }

        serialConnection.open();
        serialConnection.setBaudRate(250000);
        serialConnection.setDataBits(UsbSerialInterface.DATA_BITS_8);
        serialConnection.setParity(UsbSerialInterface.PARITY_NONE);
        serialConnection.setStopBits(1);
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
        Toast.makeText(this, "Got picture: "+id+ " "+result.side+" "+result.left+" "+result.right, Toast.LENGTH_SHORT).show();
        if (result.side == null) {
            if (curPosition == 90 && id == 1) {
                resultView.setText("No sign");
                return;
            }
            if (id == 1) {
                curPosition += 90;
                writePosition(curPosition);
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
            resultView.setText(result.side+ ": "+direction);
        }
    }

    private void ezWait(Object o) {
        try {
            synchronized (o) {
                o.wait();
            }
        } catch (InterruptedException e) { }
    }

    private UsbDevice getUsbDevice(UsbManager manager) {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

        if (deviceList.size() != 1) {
            Toast.makeText(MainActivity.this, "Found "+deviceList.size()+" Devices", Toast.LENGTH_LONG).show();
            return null;
        }

        for (String deviceName : deviceList.keySet()) {
            return deviceList.get(deviceName);
        }
        return null;
    }
}
