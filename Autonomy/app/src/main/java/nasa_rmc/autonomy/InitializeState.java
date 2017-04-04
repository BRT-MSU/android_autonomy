package nasa_rmc.autonomy;

import java.util.concurrent.TimeUnit;

/**
 * Created by atomlinson on 3/31/17.
 */

public class InitializeState implements LogicState {
    private LogicContext logicContext;

    private String status;
    public String getStatus() { return status; }

    InitializeState(LogicContext logicContext) {
        this.logicContext = logicContext;
    }

    @Override
    public void run() throws InterruptedException {
        status = "Initialized.";
        TimeUnit.SECONDS.sleep(1);
        logicContext.setLogicState(logicContext.getDriveState());
        logicContext.getLogicState().run();
    }
}
