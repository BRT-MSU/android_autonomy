package nasa_rmc.autonomy.logic;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import nasa_rmc.autonomy.network.Connection;
import nasa_rmc.autonomy.data.Coordinates;
import nasa_rmc.autonomy.data.Data;
import nasa_rmc.autonomy.data.Itinerary;
import nasa_rmc.autonomy.logic.logicState.DigState;
import nasa_rmc.autonomy.logic.logicState.DriveState;
import nasa_rmc.autonomy.logic.logicState.DumpState;
import nasa_rmc.autonomy.logic.logicState.InitializeState;
import nasa_rmc.autonomy.logic.logicState.LogicState;
import nasa_rmc.autonomy.logic.logicState.TerminateState;

/**
 * Created by atomlinson on 3/31/17.
 */

public class LogicContext {
    private Data data;
    public Data getData() { return data; }

    private Connection connection;
    public Connection getConnection() { return this.connection; }

    private LogicState logicState;
    public LogicState getLogicState() { return logicState; }
    public void setLogicState(LogicState logicState) {
        this.logicState = logicState;
    }

    private LogicState initializeState;
    public LogicState getInitializeState() { return initializeState; }

    private LogicState driveState;
    public LogicState getDriveState() { return driveState; }

    private LogicState digState;
    public LogicState getDigState() { return digState; }

    private LogicState dumpState;
    public LogicState getDumpState() { return dumpState; }

    private LogicState terminateState;
    public LogicState getTerminateState() { return terminateState; }

    private ArrayList<Coordinates> testPath = new ArrayList<>();

    public LogicContext(Data data){
        testPath.add(new Coordinates(0.0, 1.0));
        testPath.add(new Coordinates(0.0, 2.0));
        testPath.add(new Coordinates(1.0, 2.0));

        this.data = data;
        this.connection = Connection.main();

        initializeState = new InitializeState(this);
        driveState = new DriveState(this, generateItineraries());
        digState = new DigState(this);
        dumpState = new DumpState(this);
        terminateState = new TerminateState(this);

        setLogicState(initializeState);
    }

    public void start() {
        try {
            while(true) {
                if(data.getMIsConnected()) {
                    logicState.run();
                    break;
                }
            }
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Itinerary> generateItineraries() {
        // Initialize the first dig itinerary
        ArrayList<Coordinates> digPathOne = new ArrayList<>();
        digPathOne.add(new Coordinates(1.89, 1.5));
        digPathOne.add(new Coordinates(0.95, 5.91));

        Itinerary digItineraryOne = new Itinerary(digPathOne, Itinerary.ItineraryPurpose.DIG);

        // Initialize the second dig itinerary
        ArrayList<Coordinates> digPathTwo = new ArrayList<>();
        digPathTwo.add(new Coordinates(1.89, 1.5));
        digPathTwo.add(new Coordinates(1.89, 5.91));

        Itinerary digItineraryTwo = new Itinerary(digPathTwo, Itinerary.ItineraryPurpose.DIG);

        // Initialize the third dig itinerary
        ArrayList<Coordinates> digPathThree = new ArrayList<>();
        digPathThree.add(new Coordinates(1.89, 1.5));
        digPathThree.add(new Coordinates(2.84, 5.91));

        Itinerary digItineraryThree = new Itinerary(digPathThree, Itinerary.ItineraryPurpose.DIG);

        // Initialize the dump itinerary
        ArrayList<Coordinates> dumpPath = new ArrayList<>();
        dumpPath.add(new Coordinates(1.89, 1.5));
        dumpPath.add(new Coordinates(1.89, 0.38));

        Itinerary dumpItinerary = new Itinerary(dumpPath, Itinerary.ItineraryPurpose.DUMP);

        // Construct the list of itineraries
        ArrayList<Itinerary> itineraries = new ArrayList<>();
        itineraries.add(digItineraryOne);
        itineraries.add(dumpItinerary);
        itineraries.add(digItineraryTwo);
        itineraries.add(dumpItinerary);
        itineraries.add(digItineraryThree);
        itineraries.add(dumpItinerary);

        return itineraries;
    }
}
