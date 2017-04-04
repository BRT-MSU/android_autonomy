package nasa_rmc.autonomy;

/**
 * Created by atomlinson on 3/31/17.
 */

public class DumpState implements LogicState {
    private LogicContext logicContext;

    private String status;
    public String getStatus() { return status; }

    DumpState(LogicContext logicContext) {
        this.logicContext = logicContext;
    }

    @Override
    public void run() {

    }
}
