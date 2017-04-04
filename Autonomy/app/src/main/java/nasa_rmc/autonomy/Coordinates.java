package nasa_rmc.autonomy;

/**
 * Created by atomlinson on 3/31/17.
 */

public class Coordinates {
    private double x;
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    private double y;
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    Coordinates(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() { return "(" + x + ", " + y + ")"; }
}
