package nasa_rmc.autonomy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by atomlinson on 3/31/17.
 */

public class DriveState implements LogicState {
    private LogicContext logicContext;

    private int motorSpeed;

    private boolean isReverse;
    public void setReverse(boolean isReverse) { this.isReverse = isReverse; }

    private ArrayList<Itinerary> itineraries;

    // The translation of the Tango relative to the center of the robot
    private final double TANGO_X_TRANSLATION_FROM_CENTER = 0.0;
    private final double TANGO_Y_TRANSLATION_FROM_CENTER = 0.0;

    // The rotation of the Tango relative to the arena coordinate system
    private final double TANGO_ACTUAL_ROTATION_ADJUSTMENT = 90.0;

    // Distance and angle tolerances
    private final double DISTANCE_TOLERANCE = 0.15;
    private final double ANGLE_TOLERANCE = 10.0;

    // Time to sleep between drive commands
    private final int DRIVE_COMMAND_DELAY = 10;

    private String status;
    public String getStatus() { return status; }

    DriveState(LogicContext logicContext, ArrayList<Itinerary> itineraries) {
        this.logicContext = logicContext;
        this.itineraries = itineraries;

        this.motorSpeed = 25;
        this.isReverse = false;
    }

    @Override
    public void run() throws InterruptedException {
        Itinerary itinerary = itineraries.remove(0);

        while(!itinerary.isFinalDestinationReached()) {
            driveToCoordinates(itinerary.getNextCoordinates());

            if (hasArrivedAtCoordinates(itinerary.getNextCoordinates())) {
                status = "At final destination.";
                itinerary.arrivedAtCoordinates();
            }

            TimeUnit.MILLISECONDS.sleep(DRIVE_COMMAND_DELAY);
        }

        logicContext.setLogicState(logicContext.getDigState());
        logicContext.getLogicState().run();
    }

    private void driveToCoordinates(Coordinates coordinates) {
        double distance = getDistanceToCoordinates(coordinates);
        double actualAngleToCoordinates = getActualAngleToCoordinates(coordinates);

        boolean withinDistanceTolerance = distance < DISTANCE_TOLERANCE;
        boolean withinAngleTolerance = Math.abs(actualAngleToCoordinates) < ANGLE_TOLERANCE;

        if (withinDistanceTolerance) {
            // Arrived at coordinates
            status = "At " + coordinates.toString();
            return;
        }

        if (withinAngleTolerance) {
            status = "Going straight at " + Double.toString(actualAngleToCoordinates) + " to get to " + coordinates.toString();
            driveForward(motorSpeed);
        } else {
            if (actualAngleToCoordinates < 0) {
                status = "Turning left at " + Double.toString(actualAngleToCoordinates) + " to get to " + coordinates.toString();
                driveLeft(motorSpeed);
            } else {
                status = "Turning right at " + Double.toString(actualAngleToCoordinates) + " to get to " + coordinates.toString();
                driveRight(motorSpeed);
            }
        }
    }

    private boolean hasArrivedAtCoordinates(Coordinates coordinates) {
        double distance = getDistanceToCoordinates(coordinates);

        boolean withinDistanceTolerance = distance < DISTANCE_TOLERANCE;

        if (withinDistanceTolerance) {
            return true;
        } else {
            return false;
        }
    }

    // Determine the angle that the robot must rotate;
    private double getActualAngleToCoordinates(Coordinates coordinates) {
        double yaw = logicContext.getData().getYaw();

        double angleToCoordinates = getAngleToCoordinates(coordinates);

        if (isReverse) {
            angleToCoordinates = angleToCoordinates + 180;
            angleToCoordinates %= 360;
        }

        double actualAngleToCoordinates = yaw - angleToCoordinates;

        if (actualAngleToCoordinates < 0) {
            actualAngleToCoordinates += 360;
        }

        actualAngleToCoordinates %= 360;

        if (actualAngleToCoordinates > 180) {
            actualAngleToCoordinates = actualAngleToCoordinates - 360;
        }

        return actualAngleToCoordinates;
    }

    // Determine the angle between the current coordinates and the destination coordinates
    private double getAngleToCoordinates(Coordinates coordinates) {
        Coordinates currentPosition = new Coordinates(logicContext.getData().getXTranslation(),
                logicContext.getData().getYTranslation());

        double deltaX = coordinates.getX() - currentPosition.getX();
        double deltaY = coordinates.getY() - currentPosition.getY();

        double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));

        if (angle < 0) {
            angle += 360;
        }

        angle = angle % 360;

        return angle;
    }

    private double getDistanceToCoordinates(Coordinates coordinates) {
        Coordinates currentPosition = new Coordinates(logicContext.getData().getXTranslation(),
                logicContext.getData().getYTranslation());

        double deltaX = coordinates.getX() - currentPosition.getX();
        double deltaY = coordinates.getY() - currentPosition.getY();

        double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

        return distance;
    }

    private void driveForward(int speed) {
        int reverseCoefficient = 1;
        if (isReverse) { reverseCoefficient = -1; }

        ForwardingPrefix forwardingPrefix = ForwardingPrefix.MOTOR;
        Map<SubMessagePrefix, Integer> subMessages = new HashMap<>();
        subMessages.put(SubMessagePrefix.LEFT_MOTOR, reverseCoefficient * speed);
        subMessages.put(SubMessagePrefix.RIGHT_MOTOR, reverseCoefficient * speed);
        Message message = new Message(forwardingPrefix, subMessages);
        //logicContext.getConnection().sendMessage(message.getMessage());
    }

    private void driveLeft(int speed) {
        int reverseCoefficient = 1;
        if (isReverse) { reverseCoefficient = -1; }

        ForwardingPrefix forwardingPrefix = ForwardingPrefix.MOTOR;
        Map<SubMessagePrefix, Integer> subMessages = new HashMap<>();
        subMessages.put(SubMessagePrefix.LEFT_MOTOR, -1 * reverseCoefficient * speed);
        subMessages.put(SubMessagePrefix.RIGHT_MOTOR, reverseCoefficient * speed);
        Message message = new Message(forwardingPrefix, subMessages);
        //logicContext.getConnection().sendMessage(message.getMessage());
    }

    private void driveRight(int speed) {
        int reverseCoefficient = 1;
        if (isReverse) { reverseCoefficient = -1; }

        ForwardingPrefix forwardingPrefix = ForwardingPrefix.MOTOR;
        Map<SubMessagePrefix, Integer> subMessages = new HashMap<>();
        subMessages.put(SubMessagePrefix.LEFT_MOTOR, reverseCoefficient * speed);
        subMessages.put(SubMessagePrefix.RIGHT_MOTOR, -1 * reverseCoefficient * speed);
        Message message = new Message(forwardingPrefix, subMessages);
        //logicContext.getConnection().sendMessage(message.getMessage());
    }
}
