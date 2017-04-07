package nasa_rmc.autonomy.data;

import com.google.atap.tangoservice.TangoPoseData;

/**
 * Created by atomlinson on 3/31/17.
 */

public class Data {
    private boolean mIsConnected;
    public boolean getMIsConnected() { return mIsConnected; }
    public void setmIsConnected(boolean mIsConnected) { this.mIsConnected = mIsConnected; }

    private TangoPoseData mPose = new TangoPoseData();
    public void setMPose(TangoPoseData mPose) {
        this.mPose = mPose;
        TangoPoseData convertMPose = this.mPose;
        convertCameraCoordinatesToWorldCoordinates(convertMPose);
    }

    private double xTranslation;
    public double getXTranslation() { return this.xTranslation; }

    private double yTranslation;
    public double getYTranslation() { return this.yTranslation; }

    // The x translation adjustment given by initialization
    private double tangoXTranslationAdjustment = 0.0;
    public void setTangoXTranslationAdjustment(double tangoXTranslationAdjustment) {
        this.tangoXTranslationAdjustment = tangoXTranslationAdjustment;
    }

    // The y translation adjustment given by initialization
    private double tangoYTranslationAdjustment = 0.0;
    public void setTangoYTranslationAdjustment(double tangoYTranslationAdjustment) {
        this.tangoYTranslationAdjustment = tangoYTranslationAdjustment;
    }

    // The rotation adjustment given by initialization
    private double tangoAngleAdjustment = 0.0;
    public void setTangoAngleAdjustment(double tangoAngleAdjustment) {
        this.tangoAngleAdjustment = tangoAngleAdjustment;

        if(((int) tangoAngleAdjustment % 180.0) == 0) {
            cameraCoordinatesToWorldCoordinatesRotation = tangoAngleAdjustment - 90.0;
        } else {
            cameraCoordinatesToWorldCoordinatesRotation = tangoAngleAdjustment + 90.0;
        }
    }

    private double cameraCoordinatesToWorldCoordinatesRotation = 0.0;

    // Convert the tango (camera) coordinates to world coordinates
    private void convertCameraCoordinatesToWorldCoordinates(TangoPoseData mPose) {
        double cameraX = mPose.translation[TangoPoseData.INDEX_TRANSLATION_X];
        double cameraY = mPose.translation[TangoPoseData.INDEX_TRANSLATION_Y];

        double rotationAngle = cameraCoordinatesToWorldCoordinatesRotation;

        Coordinates rotatedCameraCoordinates = new Coordinates((Math.cos(rotationAngle) * cameraX) - (Math.sin(rotationAngle) * cameraY),
                (Math.sin(rotationAngle) * cameraX) + (Math.cos(rotationAngle) * cameraY));

        double rotatedCameraX = rotatedCameraCoordinates.getX();
        double rotatedCameraY = rotatedCameraCoordinates.getY();

        double translatedRotatedCameraX = rotatedCameraX + tangoXTranslationAdjustment;
        double translatedRotatedCameraY = rotatedCameraY + tangoYTranslationAdjustment;

        xTranslation = translatedRotatedCameraX;
        yTranslation = translatedRotatedCameraY;
    }

    public double getYaw() {
        // Quaternion vector components
        double w = mPose.rotation[TangoPoseData.INDEX_ROTATION_W];
        double x = mPose.rotation[TangoPoseData.INDEX_ROTATION_X];
        double y = mPose.rotation[TangoPoseData.INDEX_ROTATION_Y];
        double z = mPose.rotation[TangoPoseData.INDEX_ROTATION_Z];

        // Extract yaw from the quaternion vector components
        double yaw = Math.toDegrees(Math.atan2((2 * ((x * y) + (z * w))),
                (Math.pow(w, 2) + Math.pow(x, 2) -
                        Math.pow(y, 2) - Math.pow(z, 2))));

        if (yaw < 0) {
            yaw += 360;
        }

        yaw += tangoAngleAdjustment;

        yaw = yaw % 360;

        return yaw;
    }

    //TODO: Add obstacle avoidance here.
}