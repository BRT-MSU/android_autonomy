package com.example.angustomlinson.tangoautonomy20;

import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Joe on 2/12/2017.
 */

public class AutonomyLogic {

    // AUTONOMY VARIABLES:
    final static double DEFAULT_X = 0, DEFAULT_Y = 2.94;

    int initializationPosition;



    enum AutonomyState {
        INITIALIZE,
        DRIVE,
        DIG,
        RETURN,
        DUMP,
        DONE
    }

    enum InitialPosition {
        A_NORTH,
        A_EAST,
        A_SOUTH,
        A_WEST,
        B_NORTH,
        B_EAST,
        B_SOUTH,
        B_WEST
    }

    static final boolean gentleTurns = false;
    int leftSpeed = 0;
    int rightSpeed = 0;

    boolean autonomyActive = false;

    public double[] destinationTranslation;
    ArrayList<Double> path = new ArrayList<Double>();
    int currentPoint = -1;

    double yaw;
    double angle;
    double adjustedAngle;
    double distance;

    boolean arduinoFound;
    boolean movingBackwards;
    boolean withinAngleTolerance;
    boolean withinReverseAngleTolerance;

    AutonomyState autonomyState;
    InitialPosition startingPos;

    double ANGLE_TOLERANCE = 5;
    double DISTANCE_TOLERANCE = 0.5;
    
    AutonomyActivity autonomyActivity;
    
    public AutonomyLogic(AutonomyActivity autonomyActivity){
        this.autonomyActivity = autonomyActivity;
    }

    public void initializeDrivePath() {
        //Initialize Path to dig site
        autonomyState = AutonomyState.INITIALIZE;
        path.add(1.5);
        path.add(1.89);
        path.add(4.44);
        path.add(1.89);
        path.add(5.64);
        path.add(0.945);

        // Initialize Destination
        //double xDestination = DEFAULT_X, yDestination = DEFAULT_Y;
        destinationTranslation = new double[]{path.get(0), path.get(1), 0};
    }

    public InitialPosition getStartingPos(){
        String textInputStartingPos = autonomyActivity.angleOffsetField.getText().toString();
        InitialPosition startingPos = InitialPosition.A_NORTH;
        if(textInputStartingPos.equals("90")){
            startingPos = AutonomyLogic.InitialPosition.A_EAST;
        }else if(textInputStartingPos.equals("180")){
            startingPos =  AutonomyLogic.InitialPosition.A_SOUTH;
        }else if(textInputStartingPos.equals("270")){
            startingPos = AutonomyLogic.InitialPosition.A_WEST;
        }else if (!textInputStartingPos.equals("0")){
            Log.v(autonomyActivity.TAG, "Incorrect starting position, " + textInputStartingPos + ", no action will be taken.");
        }
        Log.v(autonomyActivity.TAG, "Got startingPos");
        return startingPos;
    }

    public void rotatePath(int numTimes){
        Log.v(autonomyActivity.TAG, "Attempting to rotate " + numTimes);
        if(numTimes == 1 || numTimes == 2 || numTimes == 3){
            //rotate 90 degrees CW by mapping each (x,y) to (y,-x)
            for(int i=0; i<path.size(); i+=2){
                double futureY = -path.get(i);
                path.set(i, path.get(i+1));
                path.set(i+1, futureY);
            }
            rotatePath(numTimes-1);
        }else if(numTimes != 0){
            Log.v(autonomyActivity.TAG, "Attempted to rotate " + numTimes + " times, invalid");
        }
    }

    public void performNextAutonomyAction() {
        switch (autonomyState) {
            case INITIALIZE:
                // Send 'R' to arduino
                //autonomyActivity.updateIR('R');

                if (true || autonomyActivity.arduinoConnection.getReceivedData() != null) {
                    if (true || !autonomyActivity.arduinoConnection.getReceivedData().isEmpty() && autonomyActivity.arduinoConnection.getReceivedData().matches("[0-9]")) {
                        try {
                            //initializationPosition = Integer.parseInt(autonomyActivity.arduinoConnection.getReceivedData());
                            startingPos = getStartingPos();
                            if(startingPos == InitialPosition.A_WEST || startingPos == InitialPosition.B_WEST){
                                rotatePath(1);
                            }else if(startingPos == InitialPosition.A_SOUTH || startingPos == InitialPosition.B_SOUTH){
                                rotatePath(2);
                            }else if(startingPos == InitialPosition.A_EAST || startingPos == InitialPosition.B_EAST){
                                rotatePath(3);
                            }
                            destinationTranslation[0] = path.get(0);
                            destinationTranslation[1] = path.get(1);
                            autonomyState = AutonomyState.DRIVE;
                        } catch (Exception ex) {
                            Log.v(autonomyActivity.TAG, "Failed to get starting pos: " + ex.getLocalizedMessage());
                        }
                    }
                }
                autonomyActivity.receivedDataView.setText("Received Data: " + autonomyActivity.arduinoConnection.getReceivedData());

                // Send ' ' to arduino
                //autonomyActivity.updateIR(' ');
                break;
            case DRIVE:
                final String driveString = drive(distance, adjustedAngle);
                autonomyActivity.adjustedAngleView.post(new Runnable() {
                    @Override
                    public void run() {
                        autonomyActivity.adjustedAngleView.setText(driveString);
                    }
                });
                break;
            case DIG:
                initiateDigProcedure();
                break;
            case RETURN:
                final String returnString = drive(distance, adjustedAngle);
                autonomyActivity.adjustedAngleView.post(new Runnable() {
                    @Override
                    public void run() {
                        autonomyActivity.adjustedAngleView.setText(returnString);
                    }
                });
                break;
            case DUMP:
                initiateDumpProcedure();
                break;
            case DONE:
                autonomyActivity.digView.post(new Runnable() {
                    @Override
                    public void run() {
                        autonomyActivity.digView.setText("Done Digging");
                    }
                });
                break;
        }
    }

    public String drive(double distance, double adjustedAngle) {

        arduinoFound = autonomyActivity.arduinoConnection.arduinoConnected();

        String adjustedAngleString;

        leftSpeed = 0;
        rightSpeed = 0;

        movingBackwards = autonomyState == AutonomyState.RETURN;

        withinAngleTolerance = Math.abs(adjustedAngle) < ANGLE_TOLERANCE;
        withinReverseAngleTolerance = movingBackwards &&
                (adjustedAngle > 180 - ANGLE_TOLERANCE || adjustedAngle < ANGLE_TOLERANCE - 180);

        if (distance > DISTANCE_TOLERANCE) {
            if (withinAngleTolerance || withinReverseAngleTolerance) {
                adjustedAngleString = "Go straight.";
                leftSpeed = (movingBackwards) ? -75 : 75;
                rightSpeed = (movingBackwards) ? -75 : 75;
            } else {
                if ((!movingBackwards && adjustedAngle < 0) || (movingBackwards && adjustedAngle > 0)) {
                    adjustedAngleString = "Go left " + autonomyActivity.decimalFormat.format(Math.abs(adjustedAngle)) + " degrees";
                    leftSpeed = (gentleTurns) ? 0 : -75;
                    rightSpeed = 75;
                } else {
                    adjustedAngleString = "Go right " + autonomyActivity.decimalFormat.format(Math.abs(adjustedAngle)) + " degrees";
                    rightSpeed = (gentleTurns) ? 0 : -75;
                    leftSpeed = 75;
                }
            }
        } else {
            adjustedAngleString = "You have arrived at your destination.";

            if (autonomyState == AutonomyState.DRIVE) {
                if (2 * (currentPoint + 1) == path.size()) {
                    autonomyState = AutonomyState.DIG;
                } else {
                    currentPoint++;
                }
            } else {
                if (currentPoint == 0) {
                    autonomyState = AutonomyState.DUMP;
                } else {
                    currentPoint--;
                }
            }
            destinationTranslation[0] = path.get(2 * currentPoint);
            destinationTranslation[1] = path.get(2 * currentPoint + 1);
        }
        updateLeft(leftSpeed);
        updateRight(rightSpeed);

        //Send new commands to Arduino
        if (arduinoFound) {
            synchronized (autonomyActivity.bufferLock){
                autonomyActivity.arduinoConnection.sendCommands();
            }
        }

        return adjustedAngleString;
    }

    public void initiateDigProcedure() {
        int downTime = 10; // 10/10 of a second
        int digTime = 100; // 3 seconds
        int upTime = 10;

        autonomyActivity.digView.post(new Runnable() {
            @Override
            public void run() {
                autonomyActivity.digView.setText("Digging");
            }
        });

        for (int i = 0; i < 5; i++) {
            // Time is in 10ths of a second
            actuateDown(downTime);
            doDig(digTime);
        }

        autonomyActivity.digView.post(new Runnable() {
            @Override
            public void run() {
                autonomyActivity.digView.setText("Has Dug, Not Digging");
            }
        });

        actuateUp(upTime);
        destinationTranslation[0] = path.get(2 * currentPoint);
        destinationTranslation[1] = path.get(2 * currentPoint + 1);
        autonomyState = AutonomyState.RETURN;
    }

    public void initiateDumpProcedure() {
        updateLeft(0);
        updateRight(0);
        int dumpTime = 100; // 10 seconds
        doDump(dumpTime);
        autonomyState = AutonomyState.DRIVE;
    }

    public void actuateDown(int time) {
        synchronized (autonomyActivity.bufferLock){
            autonomyActivity.arduinoConnection.setActuate((byte) (-80));
        }
        if (autonomyActivity.arduinoConnection.arduinoConnected()) {
            synchronized (autonomyActivity.bufferLock) {
                autonomyActivity.arduinoConnection.sendCommands();
            }
        }
        try {
            Thread.sleep(time * 100);
        } catch (Exception ignored) {

        }

        synchronized (autonomyActivity.bufferLock){
            autonomyActivity.arduinoConnection.setActuate((byte) (0));
            autonomyActivity.arduinoConnection.sendCommands();
        }
    }

    public void actuateUp(int time) {
        synchronized (autonomyActivity.bufferLock){
            autonomyActivity.arduinoConnection.setActuate((byte) (80));
        }
        if (autonomyActivity.arduinoConnection.arduinoConnected()) {
            synchronized (autonomyActivity.bufferLock){
                autonomyActivity.arduinoConnection.sendCommands();
            }
        }

        try {
            Thread.sleep(time * 100);
        } catch (Exception ignored) {

        }

        synchronized (autonomyActivity.bufferLock){
            autonomyActivity.arduinoConnection.setActuate((byte) (0));
            autonomyActivity.arduinoConnection.sendCommands();
        }
    }

    public String doDig(int time) {
        synchronized (autonomyActivity.bufferLock){
            autonomyActivity.arduinoConnection.setDig((byte) (time));
        }
        if (autonomyActivity.arduinoConnection.arduinoConnected()) {
            synchronized (autonomyActivity.bufferLock){
                autonomyActivity.arduinoConnection.sendCommands();
            }
        }

        try {
            Thread.sleep(time * 100 + 100);
        } catch (Exception ignored) {

        }

        synchronized (autonomyActivity.bufferLock) {
            autonomyActivity.arduinoConnection.setDig((byte) (0));
        }

        return null;
    }

    public String doDump(int time) {
        synchronized (autonomyActivity.bufferLock){
            autonomyActivity.arduinoConnection.setDump((byte) (time));
        }
        if (autonomyActivity.arduinoConnection.arduinoConnected()) {
            synchronized (autonomyActivity.bufferLock){
                autonomyActivity.arduinoConnection.sendCommands();
            }
        }

        try {
            Thread.sleep(time * 100 + 100);
        } catch (Exception ignored) {

        }

        synchronized (autonomyActivity.bufferLock){
            autonomyActivity.arduinoConnection.setDump((byte) (0));
        }

        return null;
    }

    public void findAdjustedAngle() {
        // display the angle between the Tango and destination
        double deltaX = destinationTranslation[0] - autonomyActivity.mPose.translation[0];
        double deltaY = destinationTranslation[1] - autonomyActivity.mPose.translation[1];

        distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

        angle = Math.toDegrees(Math.atan2(deltaY, deltaX));

        if (angle < 0) {
            angle += 360;
        }

        angle = angle % 360;

        adjustedAngle = yaw - angle;
        adjustedAngle += 360;
        adjustedAngle %= 360;

        if (adjustedAngle > 180) {
            adjustedAngle = adjustedAngle - 360;
        }
    }

    public void updateLeft(int speed) {
        synchronized (autonomyActivity.bufferLock){
            autonomyActivity.arduinoConnection.setLeftForward((byte) speed);
        }
    }

    public void updateRight(int speed) {
        synchronized (autonomyActivity.bufferLock){
            autonomyActivity.arduinoConnection.setRightForward((byte) speed);
        }
    }

    // Method called in content_main.xml when button is clicked
    public void reinitializeDestination(View view) {
        destinationTranslation[0] = autonomyActivity.mPose.translation[0] + DEFAULT_X;
        destinationTranslation[1] = autonomyActivity.mPose.translation[1] + DEFAULT_Y;
    }

}
