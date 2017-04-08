package nasa_rmc.autonomy.logic.logicState;

import java.util.concurrent.TimeUnit;

import nasa_rmc.autonomy.logic.LogicContext;
import nasa_rmc.autonomy.logic.logicState.LogicState;

/**
 * Created by atomlinson on 3/31/17.
 */

public class TerminateState implements LogicState {
    private LogicContext logicContext;

    private String status;
    public String getStatus() { return status; }

    public TerminateState(LogicContext logicContext) {
        this.logicContext = logicContext;
    }

    @Override
    public void run() {
        status = "Terminated.";
    }
}
