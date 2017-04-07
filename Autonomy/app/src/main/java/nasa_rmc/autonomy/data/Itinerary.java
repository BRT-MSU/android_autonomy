package nasa_rmc.autonomy.data;

import java.util.ArrayList;

import nasa_rmc.autonomy.data.Coordinates;

/**
 * Created by atomlinson on 3/31/17.
 */

public class Itinerary {
    private ArrayList<Coordinates> path;
    private ArrayList<Coordinates> getPath() { return path; }

    public enum ItineraryPurpose {
        DIG,
        DUMP;
    }

    private ItineraryPurpose itineraryPurpose;
    public ItineraryPurpose getItineraryPurpose() { return this.itineraryPurpose; }

    public Itinerary(ArrayList<Coordinates> path, ItineraryPurpose itineraryPurpose) {
        this.path = path;
        this.itineraryPurpose = itineraryPurpose;
    }

    public void setInitialPosition(Coordinates initialPosition) {
        path.add(0, initialPosition);
    }

    public Coordinates getNextCoordinates() { return path.get(0); }

    public void arrivedAtCoordinates() {
        path.remove(0);
    }

    public boolean isFinalDestinationReached() { return path.size() == 0; }
}
