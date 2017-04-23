package nasa_rmc.autonomy;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;
import com.projecttango.tangosupport.ux.TangoUx;
import com.projecttango.tangosupport.ux.UxExceptionEvent;
import com.projecttango.tangosupport.ux.UxExceptionEventListener;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import nasa_rmc.autonomy.data.Data;
import nasa_rmc.autonomy.logic.LogicContext;
import nasa_rmc.autonomy.logic.logicState.PictureResult;

/**
 * Main Activity class for autonomy.
 */
public class AutonomyActivity extends Activity {
    private static final String TAG = AutonomyActivity.class.getSimpleName();

    private static final String UX_EXCEPTION_EVENT_DETECTED = "Exception Detected: ";
    private static final String UX_EXCEPTION_EVENT_RESOLVED = "Exception Resolved: ";

    private static final int SECS_TO_MILLISECS = 1000;
    private static final DecimalFormat FORMAT_THREE_DECIMAL = new DecimalFormat("0.000");
    private static final double UPDATE_INTERVAL_MS = 100.0;

    private Tango mTango;
    private TangoConfig mConfig;
    private TangoUx mTangoUx;

    private TangoPointCloudManager mPointCloudManager;

    private double mPointCloudPreviousTimeStamp;

    private boolean mIsConnected = false;

    private double mPointCloudTimeToNextUpdate = UPDATE_INTERVAL_MS;

    private TangoPoseData mPose;

    // POSE UPDATE VARIABLES:
    private float mLastPoseTimeStamp = 0.0f;
    private float mCurrentPoseTimeStamp;
    private float mPoseDelta;
    private final float POSE_UPDATE_TIME = 0.1f; // in seconds

    private final Object mPoseLock = new Object();

    Data data;
    LogicContext logicContext;

    // TextViews and Buttons:
    TextView adjustedAngleView;
    TextView poseView;
    TextView angleView;
    TextView rotationView;
    TextView yawView;
    TextView depthView;
    TextView resultView;

    private SurfaceHolder holder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        mPointCloudManager = new TangoPointCloudManager();
        mTangoUx = setupTangoUxAndLayout();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 0);
        }

        holder = ((SurfaceView)findViewById(R.id.surfaceView)).getHolder();

        adjustedAngleView = (TextView) findViewById(R.id.adjustedAngleView);

        poseView = (TextView) findViewById(R.id.poseView);

        angleView = (TextView) findViewById(R.id.angleView);
        rotationView = (TextView) findViewById(R.id.rotationView);
        yawView = (TextView) findViewById(R.id.yawView);

        depthView = (TextView) findViewById(R.id.depthView);

        resultView = (TextView) findViewById(R.id.resultView);

        data = new Data();
        logicContext = new LogicContext(this, data);

        new Thread(new Runnable() {
            @Override
            public void run() {
                logicContext.start();
            }
        }).start();

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            }, null);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTangoUx.start();
        // Check and request camera permission at run time.
        bindTangoService();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Synchronize against disconnecting while the service is being used in the OpenGL
        // thread or in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread.
        // Tango.disconnect will block here until all Tango callback calls are finished.
        // If you lock against this object in a Tango callback thread it will cause a deadlock.
        synchronized (this) {
            try {
                mTangoUx.stop();
                mTango.disconnect();
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }
    }

    /**
     * Initialize Tango Service as a normal Android Service.
     */
    private void bindTangoService() {
        // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time onResume gets called we
        // should create a new Tango object.
        mTango = new Tango(AutonomyActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there are no
            // UI thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in the OpenGL
                // thread or in the UI thread.
                synchronized (AutonomyActivity.this) {
                    try {
                        TangoSupport.initialize();
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                        mIsConnected = true;
                        data.setmIsConnected(mIsConnected);
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_error);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_invalid);
                    }
                }
            }
        });
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Use the default configuration plus add depth sensing.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        return config;
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the Point Cloud and Tango Events and Pose.
     */
    private void startupTango() {
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();

        framePairs.add(new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // Passing in the pose data to UX library produce exceptions.
                if (mTangoUx != null) {
                    mTangoUx.updatePoseStatus(pose.statusCode);
                }

                // Make sure to have atomic access to Tango Pose Data so that
                // render loop doesn't interfere while Pose call back is updating
                // the data.
                synchronized (mPoseLock) {
                    mPose = pose;
                    data.setMPose(mPose);

                    mCurrentPoseTimeStamp = (float) pose.timestamp;
                    mPoseDelta = mCurrentPoseTimeStamp - mLastPoseTimeStamp;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // These textViews are strictly for debugging purposes
                        String adjustedAngleString = logicContext.getLogicState().getStatus();
                        adjustedAngleView.setText(adjustedAngleString);

                        String poseViewString = "Pose (x, y): (" + FORMAT_THREE_DECIMAL.format(data.getXTranslation()) + ", " +
                                FORMAT_THREE_DECIMAL.format(data.getYTranslation()) + ")";
                        poseView.setText(poseViewString);

                        String yawString = "Yaw (degrees): " + data.getYaw();
                        yawView.setText(yawString);
                    }
                });
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                if (mTangoUx != null) {
                    mTangoUx.updatePointCloud(pointCloud);
                }
                mPointCloudManager.updatePointCloud(pointCloud);

                final double currentTimeStamp = pointCloud.timestamp;
                final double pointCloudFrameDelta =
                        (currentTimeStamp - mPointCloudPreviousTimeStamp) * SECS_TO_MILLISECS;
                mPointCloudPreviousTimeStamp = currentTimeStamp;

                mPointCloudTimeToNextUpdate -= pointCloudFrameDelta;

                if (mPointCloudTimeToNextUpdate < 0.0) {
                    mPointCloudTimeToNextUpdate = UPDATE_INTERVAL_MS;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // If necessary, add UI debugging messages here.
                        }
                    });
                }
            }

            @Override
            public void onTangoEvent(TangoEvent event) {
                if (mTangoUx != null) {
                    mTangoUx.updateTangoEvent(event);
                }
            }
        });
    }

    /**
     * Sets up TangoUX and sets its listener.
     */
    private TangoUx setupTangoUxAndLayout() {
        TangoUx tangoUx = new TangoUx(this);
        tangoUx.setUxExceptionEventListener(mUxExceptionListener);
        return tangoUx;
    }

    /*
    * Set a UxExceptionEventListener to be notified of any UX exceptions.
    * In this example we are just logging all the exceptions to logcat, but in a real app,
    * developers should use these exceptions to contextually notify the user and help direct the
    * user in using the device in a way Tango Service expects it.
    * <p>
    * A UxExceptionEvent can have two statuses: DETECTED and RESOLVED.
    * An event is considered DETECTED when the exception conditions are observed, and RESOLVED when
    * the root causes have been addressed.
    * Both statuses will trigger a separate event.
    */
    private UxExceptionEventListener mUxExceptionListener = new UxExceptionEventListener() {
        @Override
        public void onUxExceptionEvent(UxExceptionEvent uxExceptionEvent) {
            String status = uxExceptionEvent.getStatus() == UxExceptionEvent.STATUS_DETECTED ?
                    UX_EXCEPTION_EVENT_DETECTED : UX_EXCEPTION_EVENT_RESOLVED;

            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_LYING_ON_SURFACE) {
                Log.i(TAG, status + "Device lying on surface");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_DEPTH_POINTS) {
                Log.i(TAG, status + "Too few depth points");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_FEATURES) {
                Log.i(TAG, status + "Too few features");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOTION_TRACK_INVALID) {
                Log.i(TAG, status + "Invalid poses in MotionTracking");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOVING_TOO_FAST) {
                Log.i(TAG, status + "Moving too fast");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FISHEYE_CAMERA_OVER_EXPOSED) {
                Log.i(TAG, status + "Fisheye Camera Over Exposed");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FISHEYE_CAMERA_UNDER_EXPOSED) {
                Log.i(TAG, status + "Fisheye Camera Under Exposed");
            }
        }
    };

    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private void showsToastAndFinishOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AutonomyActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

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

    public void takePicture(final int id) {
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
}
