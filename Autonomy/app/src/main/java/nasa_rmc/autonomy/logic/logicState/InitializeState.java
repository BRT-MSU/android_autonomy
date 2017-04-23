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

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import nasa_rmc.autonomy.network.message.ForwardingPrefix;
import nasa_rmc.autonomy.network.message.Message;
import nasa_rmc.autonomy.network.message.SubMessagePrefix;

/**
 * Created by atomlinson on 3/31/17.
 */

public class InitializeState implements LogicState {
    private LogicContext logicContext;

    private String status;
    public String getStatus() { return status; }

    public InitializeState(LogicContext logicContext) {
        this.logicContext = logicContext;
    }

    @Override
    public void run() throws InterruptedException {
        status = "Initializing.";
        TimeUnit.SECONDS.sleep(10);

        logicContext.getAutonomyActivity().takePicture(0);
        writeServo(0);

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

    private void writeServo(int angle) {
        ForwardingPrefix forwardingPrefix = ForwardingPrefix.MOTOR;
        Map<SubMessagePrefix, Integer> subMessages = new HashMap<>();
        subMessages.put(SubMessagePrefix.SERVO, angle);
        Message message = new Message(forwardingPrefix, subMessages);
        //logicContext.getConnection().sendMessage(message.getMessage());
    }
}
