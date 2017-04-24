package nasa_rmc.autonomy.logic.logicState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import nasa_rmc.autonomy.logic.LogicContext;
import nasa_rmc.autonomy.network.message.ForwardingPrefix;
import nasa_rmc.autonomy.network.message.Message;
import nasa_rmc.autonomy.network.message.SubMessagePrefix;

/**
 * Created by atomlinson on 3/31/17.
 */

public class InitializeState implements LogicState {
    private LogicContext logicContext;

    private String status;
    public String getStatus() { return status; }

    public InitializeState(LogicContext logicContext) {
        this.logicContext = logicContext;
    }

    @Override
    public void run() throws InterruptedException {
        status = "Initializing.";
        TimeUnit.SECONDS.sleep(1);

        logicContext.getAutonomyActivity().initialize();

        // Initialization should return variables like these (x translation, y translation, and angle)
        double tangoXTranslationAdjustment = 2.84;
        double tangoYTranslationAdjustment = 0.75;
        double tangoAngleAdjustment = 0.0;

        logicContext.getData().setTangoXTranslationAdjustment(tangoXTranslationAdjustment);
        logicContext.getData().setTangoYTranslationAdjustment(tangoYTranslationAdjustment);
        logicContext.getData().setTangoAngleAdjustment(tangoAngleAdjustment);

        logicContext.setLogicState(logicContext.getDriveState());
        logicContext.getLogicState().run();
    }
}
