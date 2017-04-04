package nasa_rmc.autonomy;

import com.google.atap.tangoservice.TangoPoseData;

/**
 * Created by atomlinson on 3/31/17.
 */

public class Data {
    private TangoPoseData mPose;

    public TangoPoseData getMPose() { return mPose; }
    public void setMPose(TangoPoseData mPose) { this.mPose = mPose; }

    public double getXTranslation() { return mPose.getTranslationAsFloats()[TangoPoseData.INDEX_TRANSLATION_X]; }
    public double getYTranslation() { return mPose.getTranslationAsFloats()[TangoPoseData.INDEX_TRANSLATION_Y]; }
    public double getZTranslation() { return mPose.getTranslationAsFloats()[TangoPoseData.INDEX_TRANSLATION_Z]; }

    // The rotation of the Tango relative to the arena coordinate system
    private final double TANGO_ACTUAL_ROTATION_ADJUSTMENT = 90.0;

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

        yaw += TANGO_ACTUAL_ROTATION_ADJUSTMENT;

        yaw = yaw % 360;

        return yaw;
    }

    //TODO: Add obstacle avoidance here.
}