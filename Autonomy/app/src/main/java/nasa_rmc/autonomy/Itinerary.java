package nasa_rmc.autonomy;

import java.util.ArrayList;

/**
 * Created by atomlinson on 3/31/17.
 */

public class Itinerary {
    private ArrayList<Coordinates> path;

    private ArrayList<Coordinates> getPath() { return path; }

    public boolean isFinalDestinationReached() { return path.size() == 0; }

    Itinerary(ArrayList<Coordinates> path) {
        this.path = path;
    }

    public void setInitialPosition(Coordinates initialPosition) {
        path.add(0, initialPosition);
    }

    public Coordinates getNextCoordinates() { return path.get(0); }

    public void arrivedAtCoordinates() {
        path.remove(0);
    }
}
