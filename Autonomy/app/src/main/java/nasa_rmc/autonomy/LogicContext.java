package nasa_rmc.autonomy;

import java.sql.Time;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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

    ArrayList<Coordinates> testPath = new ArrayList<>();

    LogicContext(Data data){
        testPath.add(new Coordinates(0.0, 1.0));
        testPath.add(new Coordinates(0.0, 2.0));
        testPath.add(new Coordinates(1.0, 2.0));

        Itinerary testItinerary = new Itinerary(testPath);

        ArrayList<Itinerary> testItineraries = new ArrayList<>();
        testItineraries.add(testItinerary);

        this.data = data;
        this.connection = Connection.main();

        initializeState = new InitializeState(this);
        driveState = new DriveState(this, testItineraries);
        digState = new DigState(this);
        dumpState = new DumpState(this);
        terminateState = new TerminateState(this);

        setLogicState(initializeState);
    }

    public void start() {
        try {
            TimeUnit.SECONDS.sleep(10);
            logicState.run();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}
