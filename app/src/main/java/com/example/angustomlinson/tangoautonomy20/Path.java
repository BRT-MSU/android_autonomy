package com.example.angustomlinson.tangoautonomy20;

import java.util.ArrayList;

/**
 * Created by Alex on 5/17/2016.
 */

public class Path {
    private ArrayList<Double> pointsX, pointsY;
    private ArrayList<Boolean> forwards;

    private enum PathSegment {
        A_NORTH,
        A_EAST,
        A_SOUTH,
        A_WEST,
        B_NORTH,
        B_EAST,
        B_SOUTH,
        B_WEST,
        F_POST_INIT,
        B_POST_INIT,
        DRIVE
    }

    public Path() {
        pointsX = new ArrayList<>();
        pointsY = new ArrayList<>();
        forwards = new ArrayList<>();
    }

    public Path(double[] xValues, double[] yValues, boolean[] dirValues) {
        ArrayList<Double> xList = new ArrayList();
        for(double d: xValues)
            xList.add(d);
        pointsX = xList;

        ArrayList<Double> yList = new ArrayList();
        for(double d: yValues)
            yList.add(d);
        pointsY = yList;

        ArrayList<Boolean> dirList = new ArrayList();
        for(boolean b: dirValues)
            dirList.add(b);
        forwards = dirList;
    }

    public void addPoint(double x, double y) {
        pointsX.add(x);
        pointsY.add(y);
    }

    public double[] getPoint(int i) {
        return new double[] {pointsX.get(i), pointsY.get(i)};
    }

    public boolean getPointDir(int i) {
        return forwards.get(i);
    }

    public void reset() {
        pointsX = new ArrayList<>();
        pointsY = new ArrayList<>();
        forwards = new ArrayList<>();
    }

    public int size() {
        return pointsY.size();
    }

    public Path getPath(PathSegment initialPosition) {
        Path path = null;
        switch (initialPosition) {
            case A_NORTH:
                path = new Path(A_NORTH_X, A_NORTH_Y, A_NORTH_D);
                break;
            case A_EAST:
                path = new Path(A_EAST_X, A_EAST_Y, A_EAST_D);
                break;
            case A_SOUTH:
                path = new Path(A_SOUTH_X, A_SOUTH_Y, A_SOUTH_D);
                break;
            case A_WEST:
                path = new Path(A_SOUTH_X, A_SOUTH_Y, A_SOUTH_D);
                break;
            case B_NORTH:
                path = new Path(B_NORTH_X, B_NORTH_Y, B_NORTH_D);
                break;
            case B_EAST:
                path = new Path(B_EAST_X, B_EAST_Y, B_EAST_D);
                break;
            case B_SOUTH:
                path = new Path(B_SOUTH_X, B_SOUTH_Y, B_SOUTH_D);
                break;
            case B_WEST:
                path = new Path(B_WEST_X, B_WEST_Y, B_WEST_D);
                break;
            case F_POST_INIT:
                path = new Path(F_POST_INIT_X, F_POST_INIT_Y, F_POST_INIT_D);
                break;
            case B_POST_INIT:
                path = new Path(B_POST_INIT_X, B_POST_INIT_Y, B_POST_INIT_D);
                break;
            case DRIVE:
                path = new Path(DRIVE_X, DRIVE_Y, DRIVE_D);
                break;
        }

        return path;
    }

    private final static double[] A_NORTH_X = {1.4175, 1.89, 2.3625, 2.835, 2.3625};
    private final static double[] A_NORTH_Y = {1.125, 1.5, 1.125, 0.75, 1.125};
    private final static boolean[] A_NORTH_D = {false, false, false, false, true};

    private final static double[] A_EAST_X = {1.89, 2.835, 2.3635, 1.89};
    private final static double[] A_EAST_Y = {.75, .75, 1.125, 1.5};
    private final static boolean[] A_EAST_D = {false, false, true, true};

    private final static double[] A_SOUTH_X =  {1.4175, 1.89};
    private final static double[] A_SOUTH_Y =  {1.125, 1.5};
    private final static boolean[] A_SOUTH_D = {true, true};

    private final static double[] A_WEST_X = {1.4171, 1.89};
    private final static double[] A_WEST_Y = {1.125, 1.5};
    private final static boolean[] A_WEST_D = {true, true};


    private final static double[] B_NORTH_X = {2.3625, 1.89, 1.4175, 0.945, 1.4175};
    private final static double[] B_NORTH_Y = {1.125, 1.5, 1.125, 0.75, 1.125};
    private final static boolean[] B_NORTH_D = {false, false, false, false, true};

    private final static double[] B_EAST_X = {2.3625, 1.89};
    private final static double[] B_EAST_Y = {1.125, 1.5};
    private final static boolean[] B_EAST_D = {false, false};

    private final static double[] B_SOUTH_X = {2.3625, 1.89, 1.4175, 0.945, 1.4175};
    private final static double[] B_SOUTH_Y = {1.125, 1.5, 1.125, 0.75, 1.125};
    private final static boolean[] B_SOUTH_D = {false, false, false, false, true};

    private final static double[] B_WEST_X = {2.3625, 1.89, 1.4175, 0.945, 1.4175};
    private final static double[] B_WEST_Y = {1.125, 1.5, 1.125, 0.75, 1.125};
    private final static boolean[] B_WEST_D = {false, false, false, false, true};


    private final static double[] F_POST_INIT_X = {1.89, 1.89};
    private final static double[] F_POST_INIT_Y = {.75, 1.5};
    private final static boolean[] F_POST_INIT_D = {true, true};

    private final static double[] B_POST_INIT_X = {1.89, 1.89};
    private final static double[] B_POST_INIT_Y = {.75, 1.5};
    private final static boolean[] B_POST_INIT_D = {false, false};

    private final static double[] DRIVE_X = {1.89, 1.89};
    private final static double[] DRIVE_Y = {.75, 1.5};
    private final static boolean[] DRIVE_D = {true, true};
}