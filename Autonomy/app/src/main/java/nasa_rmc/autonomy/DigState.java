package nasa_rmc.autonomy;

/**
 * Created by atomlinson on 3/31/17.
 */

public class DigState implements LogicState {
    private LogicContext logicContext;

    private String status;
    public String getStatus() { return status; }

    DigState(LogicContext logicContext) {
        this.logicContext = logicContext;
    }

    @Override
    public void run() {

    }
}
