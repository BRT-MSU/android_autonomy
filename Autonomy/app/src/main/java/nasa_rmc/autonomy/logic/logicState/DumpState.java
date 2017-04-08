package nasa_rmc.autonomy.logic.logicState;

import java.util.concurrent.TimeUnit;

import nasa_rmc.autonomy.logic.LogicContext;

/**
 * Created by atomlinson on 3/31/17.
 */

public class DumpState implements LogicState {
    private LogicContext logicContext;

    private String status;
    public String getStatus() { return status; }

    public DumpState(LogicContext logicContext) {
        this.logicContext = logicContext;
    }

    @Override
    public void run() throws InterruptedException {
        status = "Dumping.";
        TimeUnit.SECONDS.sleep(2);
        logicContext.setLogicState(logicContext.getDriveState());
        logicContext.getLogicState().run();
    }
}
