package nasa_rmc.autonomy;

import java.util.Map;

/**
 * Created by atomlinson on 4/3/17.
 */

public final class Message {
    private String message;
    public String getMessage() { return message; }

    // Constructor expects a forwardingPrefix and a subMessage map.
    Message(ForwardingPrefix forwardingPrefix, Map<SubMessagePrefix, Integer> subMessages){
        String subMessageString = "";

        for (SubMessagePrefix key: subMessages.keySet()) {
            subMessageString += key.toString() + Integer.toString(subMessages.get(key)) + "|";
        }

        this.message = forwardingPrefix.toString() + subMessageString;
    }
}
